package com.example.Assignment_2.model;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Data;

/**
 * Real-time metrics for the fan-out engine.
 */
@Data
public class Metrics {
    private final AtomicLong recordsProcessed;
    private final AtomicLong recordsSucceeded;
    private final AtomicLong recordsFailed;
    private final Map<SinkType, AtomicLong> successBySink;
    private final Map<SinkType, AtomicLong> failureBySink;
    private final Instant startTime;
    
    public Metrics() {
        this.recordsProcessed = new AtomicLong(0);
        this.recordsSucceeded = new AtomicLong(0);
        this.recordsFailed = new AtomicLong(0);
        this.successBySink = new ConcurrentHashMap<>();
        this.failureBySink = new ConcurrentHashMap<>();
        this.startTime = Instant.now();
    }
    
    public void recordSuccess(SinkType sinkType) {
        recordsSucceeded.incrementAndGet();
        successBySink.computeIfAbsent(sinkType, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    public void recordFailure(SinkType sinkType) {
        recordsFailed.incrementAndGet();
        failureBySink.computeIfAbsent(sinkType, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    public void recordProcessed() {
        recordsProcessed.incrementAndGet();
    }
    
    public long getElapsedSeconds() {
        return (System.currentTimeMillis() - startTime.toEpochMilli()) / 1000;
    }
    
    public double getThroughput() {
        long elapsed = getElapsedSeconds();
        if (elapsed == 0) return 0;
        return (double) recordsProcessed.get() / elapsed;
    }
}
