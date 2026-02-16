package com.example.Assignment_2.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents an event to be sent to a sink.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SinkEvent {
    private String eventId;
    private DataRecord record;
    private SinkType sinkType;
    private Object transformedData; // Can be JSON, Protobuf, XML, Avro
    private Instant createdAt;
    private int retryCount;
    private String lastError;
    
    public boolean canRetry() {
        return retryCount < 3;
    }
    
    public void incrementRetry(String error) {
        this.retryCount++;
        this.lastError = error;
    }
}
