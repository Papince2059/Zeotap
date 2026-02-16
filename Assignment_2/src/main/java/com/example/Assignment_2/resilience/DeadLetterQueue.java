package com.example.Assignment_2.resilience;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.example.Assignment_2.model.SinkEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Dead Letter Queue (DLQ) for handling failed events that exceed retry limits.
 * Persists failed events for later analysis and recovery.
 */
@Slf4j
public class DeadLetterQueue {
    
    private final Queue<SinkEvent> queue = new ConcurrentLinkedQueue<>();
    private final String filePath;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    public DeadLetterQueue(String filePath) {
        this.filePath = filePath;
        try {
            // Create DLQ directory if not exists
            Files.createDirectories(Paths.get(filePath).getParent());
        } catch (IOException e) {
            log.error("Failed to create DLQ directory", e);
        }
    }
    
    /**
     * Adds a failed event to the DLQ.
     * @param event The failed event
     */
    public void add(SinkEvent event) {
        lock.writeLock().lock();
        try {
            queue.add(event);
            persistEvent(event);
            log.warn("Event added to DLQ: {} (Sink: {}, Retries: {})", 
                    event.getEventId(), event.getSinkType(), event.getRetryCount());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Returns the current size of the DLQ.
     * @return Number of events in DLQ
     */
    public int size() {
        lock.readLock().lock();
        try {
            return queue.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Persists an event to disk.
     * @param event The event to persist
     */
    private void persistEvent(SinkEvent event) {
        try (java.io.FileWriter fileWriter = new java.io.FileWriter(filePath, true);
             java.io.BufferedWriter writer = new java.io.BufferedWriter(fileWriter)) {
            writer.write(String.format("%s|%s|%s|%d|%s\n",
                    event.getEventId(),
                    event.getSinkType(),
                    event.getRecord().getId(),
                    event.getRetryCount(),
                    event.getLastError()));
            writer.flush();
        } catch (IOException e) {
            log.error("Failed to persist event to DLQ file", e);
        }
    }
    
    /**
     * Closes the DLQ and releases resources.
     */
    public void close() {
        log.info("Dead Letter Queue closed. Total failed events: {}", queue.size());
    }
}
