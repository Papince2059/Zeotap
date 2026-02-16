package com.example.Assignment_2.config;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration for a single sink.
 */
@Data
@Builder
public class SinkConfig {
    private String sinkType;
    private String endpoint;
    private long rateLimit; // requests per second
    private int bufferSize;
    private int maxRetries;
    private boolean enabled;
}
