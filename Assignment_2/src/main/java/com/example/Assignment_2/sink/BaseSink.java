package com.example.Assignment_2.sink;

import com.example.Assignment_2.model.SinkEvent;
import com.example.Assignment_2.model.ProcessingResult;

import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for all sink implementations.
 * Defines the contract that all sinks must implement.
 */
public abstract class BaseSink {
    
    protected final String name;
    protected final long rateLimitPerSec;
    
    public BaseSink(String name, long rateLimitPerSec) {
        this.name = name;
        this.rateLimitPerSec = rateLimitPerSec;
    }
    
    /**
     * Asynchronously sends an event to the sink.
     * @param event The event to send
     * @return CompletableFuture with the processing result
     */
    public abstract CompletableFuture<ProcessingResult> send(SinkEvent event);
    
    /**
     * Gets the name of this sink.
     * @return Sink name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the rate limit (requests per second) for this sink.
     * @return Rate limit in requests per second
     */
    public long getRateLimitPerSec() {
        return rateLimitPerSec;
    }
    
    /**
     * Closes the sink and releases any resources.
     */
    public abstract void close();
}
