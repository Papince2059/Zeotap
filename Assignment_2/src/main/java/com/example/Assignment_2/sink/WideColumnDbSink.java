package com.example.Assignment_2.sink;

import com.example.Assignment_2.model.SinkEvent;
import com.example.Assignment_2.model.ProcessingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock Wide-Column Database Sink that simulates async UPSERT operations.
 * Compatible with Cassandra, Aerospike, DynamoDB, and ScyllaDB.
 */
@Slf4j
@Component
public class WideColumnDbSink extends BaseSink {
    
    private final AtomicLong upsertCount = new AtomicLong(0);
    private final String tableName;
    
    public WideColumnDbSink() {
        super("Wide-Column DB Sink", 1000); // 1000 UPSERTs per second
        this.tableName = "fan_out_events";
    }
    
    @Override
    public CompletableFuture<ProcessingResult> send(SinkEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                // Simulate async UPSERT operation
                simulateDbOperation();
                
                long processingTime = System.currentTimeMillis() - startTime;
                upsertCount.incrementAndGet();
                
                log.debug("WideColumnDB: UPSERT event {} into table: {}", event.getEventId(), tableName);
                return ProcessingResult.success(event.getEventId(), event.getSinkType(), processingTime);
            } catch (Exception e) {
                long processingTime = System.currentTimeMillis() - startTime;
                log.error("WideColumnDB: Failed to UPSERT event {}", event.getEventId(), e);
                return ProcessingResult.failure(event.getEventId(), event.getSinkType(),
                    "UPSERT failed: " + e.getMessage(), e, processingTime);
            }
        });
    }
    
    @Override
    public void close() {
        log.info("Wide-Column DB Sink closed. Total UPSERTs performed: {}", upsertCount.get());
    }
    
    private void simulateDbOperation() throws InterruptedException {
        // Simulate database latency (1-10ms)
        long delay = 1 + (long)(Math.random() * 9);
        Thread.sleep(delay);
    }
}
