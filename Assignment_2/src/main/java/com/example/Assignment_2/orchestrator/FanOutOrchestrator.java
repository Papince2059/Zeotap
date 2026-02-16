package com.example.Assignment_2.orchestrator;

import com.example.Assignment_2.config.FanOutConfig;
import com.example.Assignment_2.config.SinkConfig;
import com.example.Assignment_2.ingestion.FileReaderFactory;
import com.example.Assignment_2.model.DataRecord;
import com.example.Assignment_2.model.ProcessingResult;
import com.example.Assignment_2.model.SinkEvent;
import com.example.Assignment_2.model.SinkType;
import com.example.Assignment_2.observability.MetricsCollector;
import com.example.Assignment_2.resilience.DeadLetterQueue;
import com.example.Assignment_2.sink.BaseSink;
import com.example.Assignment_2.sink.GrpcSink;
import com.example.Assignment_2.sink.MessageQueueSink;
import com.example.Assignment_2.sink.RestApiSink;
import com.example.Assignment_2.sink.WideColumnDbSink;
import com.example.Assignment_2.throttling.BackpressureBuffer;
import com.example.Assignment_2.throttling.RateLimiter;
import com.example.Assignment_2.transformation.TransformerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main orchestrator for the fan-out engine.
 * Coordinates data ingestion, transformation, and distribution to multiple sinks.
 */
@Slf4j
@Component
public class FanOutOrchestrator {
    
    private final FileReaderFactory fileReaderFactory;
    private final TransformerFactory transformerFactory;
    private final MetricsCollector metricsCollector;
    private final FanOutConfig config;
    private final Map<SinkType, BaseSink> sinks;
    private final Map<SinkType, RateLimiter> rateLimiters;
    private final Map<SinkType, BackpressureBuffer> buffers;
    private final DeadLetterQueue deadLetterQueue;
    private final ExecutorService executorService;
    private final AtomicLong recordCounter = new AtomicLong(0);
    
    @Autowired
    public FanOutOrchestrator(FileReaderFactory fileReaderFactory,
                            TransformerFactory transformerFactory,
                            MetricsCollector metricsCollector,
                            FanOutConfig config,
                            RestApiSink restApiSink,
                            GrpcSink grpcSink,
                            MessageQueueSink messageQueueSink,
                            WideColumnDbSink wideColumnDbSink) {
        this.fileReaderFactory = fileReaderFactory;
        this.transformerFactory = transformerFactory;
        this.metricsCollector = metricsCollector;
        this.config = config;
        
        this.sinks = new HashMap<>();
        sinks.put(SinkType.REST_API, restApiSink);
        sinks.put(SinkType.GRPC, grpcSink);
        sinks.put(SinkType.MESSAGE_QUEUE, messageQueueSink);
        sinks.put(SinkType.WIDE_COLUMN_DB, wideColumnDbSink);
        
        this.rateLimiters = new HashMap<>();
        this.buffers = new HashMap<>();
        initializeSinks();
        
        this.deadLetterQueue = new DeadLetterQueue("./data/dlq.txt");
        this.executorService = createExecutorService();
    }
    
    /**
     * Starts the fan-out process.
     * Reads from source file and dispatches to all configured sinks.
     */
    public void start() {
        log.info("Starting Fan-Out Orchestrator");
        log.info("Input file: {}", config.getInput().getFilePath());
        
        // Start metrics collector thread
        startMetricsThread();
        
        try {
            // Read records from source file
            fileReaderFactory.readRecords(config.getInput().getFilePath())
                    .forEach(this::processRecord);
            
            // Wait for all pending operations to complete
            int timeoutSeconds = 300;
            boolean completed = executorService.awaitTermination(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!completed) {
                log.warn("Executor service did not terminate within {} seconds", timeoutSeconds);
                executorService.shutdownNow();
            }
            
            metricsCollector.printStatus();
        } catch (InterruptedException e) {
            log.error("Orchestrator interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            cleanup();
        }
    }
    
    /**
     * Processes a single record by distributing it to all enabled sinks.
     * @param record The data record to process
     */
    private void processRecord(DataRecord record) {
        record.setId(UUID.randomUUID().toString());
        record.setSequenceNumber(recordCounter.incrementAndGet());
        metricsCollector.recordProcessed();
        
        for (SinkType sinkType : SinkType.values()) {
            SinkConfig sinkConfig = config.getSinks().get(sinkType.getKey());
            if (sinkConfig == null || !sinkConfig.isEnabled()) {
                continue;
            }
            
            // Create event and dispatch asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    SinkEvent event = createEvent(record, sinkType);
                    dispatchToSink(event, sinkType);
                } catch (Exception e) {
                    log.error("Error processing record for sink {}", sinkType, e);
                }
            }, executorService);
        }
    }
    
    /**
     * Dispatches an event to a sink with retry logic and backpressure handling.
     * @param event The event to dispatch
     * @param sinkType The target sink type
     */
    private void dispatchToSink(SinkEvent event, SinkType sinkType) {
        BaseSink sink = sinks.get(sinkType);
        RateLimiter rateLimiter = rateLimiters.get(sinkType);
        SinkConfig sinkConfig = config.getSinks().get(sinkType.getKey());
        
        try {
            // Apply rate limiting
            rateLimiter.acquire();
            
            // Send to sink
            CompletableFuture<ProcessingResult> future = sink.send(event);
            
            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    handleError(event, sinkType, exception, sinkConfig);
                } else if (result != null) {
                    handleResult(result, event, sinkType, sinkConfig);
                }
            });
        } catch (InterruptedException e) {
            log.error("Rate limiter interrupted for sink {}", sinkType, e);
            handleError(event, sinkType, e, sinkConfig);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Handles successful or failed processing results.
     * @param result The processing result
     * @param event The original event
     * @param sinkType The sink type
     * @param sinkConfig The sink configuration
     */
    private void handleResult(ProcessingResult result, SinkEvent event, 
                            SinkType sinkType, SinkConfig sinkConfig) {
        metricsCollector.recordResult(result);
        
        if (!result.isSuccess()) {
            handleError(event, sinkType, result.getException(), sinkConfig);
        } else {
            log.debug("Successfully processed event {} for sink {}", 
                    event.getEventId(), sinkType);
        }
    }
    
    /**
     * Handles processing errors with retry logic.
     * @param event The event that failed
     * @param sinkType The sink type
     * @param exception The exception that occurred
     * @param sinkConfig The sink configuration
     */
    private void handleError(SinkEvent event, SinkType sinkType, 
                           Throwable exception, SinkConfig sinkConfig) {
        event.incrementRetry(exception.getMessage());
        
        if (event.canRetry()) {
            log.warn("Retrying event {} for sink {} (attempt {}/3)", 
                    event.getEventId(), sinkType, event.getRetryCount());
            
            // Retry with exponential backoff
            long delayMs = (long) Math.pow(2, event.getRetryCount()) * 100;
            CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS, executorService)
                    .execute(() -> dispatchToSink(event, sinkType));
        } else {
            log.error("Event {} failed for sink {} - exceeded max retries. Error: {}", 
                    event.getEventId(), sinkType, exception.getMessage());
            deadLetterQueue.add(event);
        }
    }
    
    /**
     * Creates a SinkEvent from a DataRecord.
     * @param record The data record
     * @param sinkType The target sink type
     * @return A new SinkEvent
     */
    private SinkEvent createEvent(DataRecord record, SinkType sinkType) {
        Object transformedData = transformerFactory.getTransformer(sinkType)
                .transform(record);
        
        return SinkEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .record(record)
                .sinkType(sinkType)
                .transformedData(transformedData)
                .createdAt(java.time.Instant.now())
                .retryCount(0)
                .build();
    }
    
    /**
     * Initializes sinks with rate limiters and buffers.
     */
    private void initializeSinks() {
        for (SinkType sinkType : SinkType.values()) {
            SinkConfig sinkConfig = config.getSinks().get(sinkType.getKey());
            if (sinkConfig == null || !sinkConfig.isEnabled()) {
                continue;
            }
            
            BaseSink sink = sinks.get(sinkType);
            RateLimiter rateLimiter = new RateLimiter(sinkConfig.getRateLimit());
            BackpressureBuffer buffer = new BackpressureBuffer(
                    sinkConfig.getBufferSize(), 
                    sinkType.getDisplayName());
            
            rateLimiters.put(sinkType, rateLimiter);
            buffers.put(sinkType, buffer);
            
            log.info("Initialized sink: {} with rate limit: {} req/sec and buffer size: {}",
                    sinkType.getDisplayName(), sinkConfig.getRateLimit(), sinkConfig.getBufferSize());
        }
    }
    
    /**
     * Creates a thread pool executor for concurrent processing.
     * @return Configured ExecutorService
     */
    private ExecutorService createExecutorService() {
        FanOutConfig.ThreadPoolConfig poolConfig = config.getThreadPool();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                poolConfig.getCorePoolSize(),
                poolConfig.getMaxPoolSize(),
                poolConfig.getKeepAliveSeconds(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(poolConfig.getQueueCapacity()),
                r -> {
                    Thread t = new Thread(r, "FanOut-Worker-" + recordCounter.get());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        executor.allowCoreThreadTimeOut(true);
        log.info("Created thread pool: core={}, max={}, queue={}",
                poolConfig.getCorePoolSize(), poolConfig.getMaxPoolSize(), poolConfig.getQueueCapacity());
        
        return executor;
    }
    
    /**
     * Starts a background thread that periodically prints status metrics.
     */
    private void startMetricsThread() {
        Thread metricsThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(config.getMetricsIntervalSeconds() * 1000);
                    metricsCollector.printStatus();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Metrics-Reporter");
        metricsThread.setDaemon(true);
        metricsThread.start();
    }
    
    /**
     * Cleans up resources and closes all sinks.
     */
    private void cleanup() {
        log.info("Cleaning up resources");
        
        // Close all sinks
        sinks.values().forEach(BaseSink::close);
        
        // Close DLQ
        deadLetterQueue.close();
        
        // Shutdown executor
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("Cleanup completed");
    }
}
