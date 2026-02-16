package com.example.Assignment_2.exception;

/**
 * Exception thrown when sending data to a sink.
 */
public class SinkException extends FanOutException {
    public SinkException(String message) {
        super(message);
    }
    
    public SinkException(String message, Throwable cause) {
        super(message, cause);
    }
}
