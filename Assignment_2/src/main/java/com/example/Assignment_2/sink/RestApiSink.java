package com.example.Assignment_2.sink;

import com.example.Assignment_2.model.SinkEvent;
import com.example.Assignment_2.model.ProcessingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock REST API Sink that simulates sending data to an HTTP endpoint.
 * Implements rate limiting and simulates network delays.
 */
@Slf4j
@Component
public class RestApiSink extends BaseSink {
    
    private final AtomicLong requestCount = new AtomicLong(0);
    private final String endpoint;
    
    public RestApiSink() {
        super("REST API Sink", 50); // 50 requests per second
        this.endpoint = "http://mock-api.example.com/events";
    }
    
    @Override
    public CompletableFuture<ProcessingResult> send(SinkEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                // Simulate HTTP POST request
                simulateHttpDelay();
                
                long processingTime = System.currentTimeMillis() - startTime;
                requestCount.incrementAndGet();
                
                log.debug("REST API: Successfully sent event {} to {}", event.getEventId(), endpoint);
                return ProcessingResult.success(event.getEventId(), event.getSinkType(), processingTime);
            } catch (Exception e) {
                long processingTime = System.currentTimeMillis() - startTime;
                log.error("REST API: Failed to send event {}", event.getEventId(), e);
                return ProcessingResult.failure(event.getEventId(), event.getSinkType(), 
                    "HTTP request failed: " + e.getMessage(), e, processingTime);
            }
        });
    }
    
    @Override
    public void close() {
        log.info("REST API Sink closed. Total requests sent: {}", requestCount.get());
    }
    
    private void simulateHttpDelay() throws InterruptedException {
        // Simulate network latency (5-20ms)
        long delay = 5 + (long)(Math.random() * 15);
        Thread.sleep(delay);
    }
}
