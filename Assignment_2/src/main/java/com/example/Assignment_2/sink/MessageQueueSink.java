package com.example.Assignment_2.sink;

import com.example.Assignment_2.model.SinkEvent;
import com.example.Assignment_2.model.ProcessingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock Message Queue Sink that simulates publishing to Kafka/RabbitMQ.
 * Implements asynchronous publishing with configurable throughput.
 */
@Slf4j
@Component
public class MessageQueueSink extends BaseSink {
    
    private final AtomicLong messageCount = new AtomicLong(0);
    private final String topic;
    
    public MessageQueueSink() {
        super("Message Queue Sink", 500); // 500 messages per second
        this.topic = "fanout_events";
    }
    
    @Override
    public CompletableFuture<ProcessingResult> send(SinkEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                // Simulate message publication
                simulatePublish();
                
                long processingTime = System.currentTimeMillis() - startTime;
                messageCount.incrementAndGet();
                
                log.debug("MessageQueue: Published event {} to topic: {}", event.getEventId(), topic);
                return ProcessingResult.success(event.getEventId(), event.getSinkType(), processingTime);
            } catch (Exception e) {
                long processingTime = System.currentTimeMillis() - startTime;
                log.error("MessageQueue: Failed to publish event {}", event.getEventId(), e);
                return ProcessingResult.failure(event.getEventId(), event.getSinkType(),
                    "Publish failed: " + e.getMessage(), e, processingTime);
            }
        });
    }
    
    @Override
    public void close() {
        log.info("Message Queue Sink closed. Total messages published: {}", messageCount.get());
    }
    
    private void simulatePublish() throws InterruptedException {
        // Simulate broker latency (1-5ms)
        long delay = 1 + (long)(Math.random() * 4);
        Thread.sleep(delay);
    }
}
