package com.example.Assignment_2.sink;

import com.example.Assignment_2.model.SinkEvent;
import com.example.Assignment_2.model.ProcessingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock gRPC Sink that simulates sending data via gRPC.
 * Implements bi-directional streaming simulation and rate limiting.
 */
@Slf4j
@Component
public class GrpcSink extends BaseSink {
    
    private final AtomicLong requestCount = new AtomicLong(0);
    private final String grpcEndpoint;
    
    public GrpcSink() {
        super("gRPC Sink", 100); // 100 requests per second
        this.grpcEndpoint = "localhost:50051";
    }
    
    @Override
    public CompletableFuture<ProcessingResult> send(SinkEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                // Simulate gRPC streaming
                simulateGrpcDelay();
                
                long processingTime = System.currentTimeMillis() - startTime;
                requestCount.incrementAndGet();
                
                log.debug("gRPC: Successfully sent event {} to {}", event.getEventId(), grpcEndpoint);
                return ProcessingResult.success(event.getEventId(), event.getSinkType(), processingTime);
            } catch (Exception e) {
                long processingTime = System.currentTimeMillis() - startTime;
                log.error("gRPC: Failed to send event {}", event.getEventId(), e);
                return ProcessingResult.failure(event.getEventId(), event.getSinkType(),
                    "gRPC request failed: " + e.getMessage(), e, processingTime);
            }
        });
    }
    
    @Override
    public void close() {
        log.info("gRPC Sink closed. Total requests sent: {}", requestCount.get());
    }
    
    private void simulateGrpcDelay() throws InterruptedException {
        // Simulate gRPC latency (2-10ms, typically faster than REST)
        long delay = 2 + (long)(Math.random() * 8);
        Thread.sleep(delay);
    }
}
