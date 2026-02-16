package com.example.Assignment_2.exception;

/**
 * Base exception for fan-out engine errors.
 */
public class FanOutException extends RuntimeException {
    public FanOutException(String message) {
        super(message);
    }
    
    public FanOutException(String message, Throwable cause) {
        super(message, cause);
    }
}
