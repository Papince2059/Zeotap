package com.example.Assignment_2.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of processing a record through a sink.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessingResult {
    private String eventId;
    private SinkType sinkType;
    private boolean success;
    private String message;
    private long processingTimeMs;
    private Throwable exception;
    
    public static ProcessingResult success(String eventId, SinkType sinkType, long processingTimeMs) {
        return ProcessingResult.builder()
                .eventId(eventId)
                .sinkType(sinkType)
                .success(true)
                .message("Successfully processed")
                .processingTimeMs(processingTimeMs)
                .build();
    }
    
    public static ProcessingResult failure(String eventId, SinkType sinkType, 
                                          String message, Throwable exception, long processingTimeMs) {
        return ProcessingResult.builder()
                .eventId(eventId)
                .sinkType(sinkType)
                .success(false)
                .message(message)
                .exception(exception)
                .processingTimeMs(processingTimeMs)
                .build();
    }
}
