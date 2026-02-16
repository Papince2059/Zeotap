package com.example.Assignment_2.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Configuration properties for the fan-out engine.
 * Loaded from application.yaml or application.properties.
 */
@Component
@ConfigurationProperties(prefix = "fanout")
@Data
public class FanOutConfig {
    
    private InputConfig input = new InputConfig();
    private Map<String, SinkConfig> sinks;
    private ThreadPoolConfig threadPool = new ThreadPoolConfig();
    private long metricsIntervalSeconds = 5;
    
    @Data
    public static class InputConfig {
        private String filePath;
        private String fileType; // csv, jsonl, fixed-width
        private int batchSize = 100;
    }
    
    @Data
    public static class ThreadPoolConfig {
        private int corePoolSize = 10;
        private int maxPoolSize = 50;
        private int queueCapacity = 1000;
        private long keepAliveSeconds = 60;
    }
}
