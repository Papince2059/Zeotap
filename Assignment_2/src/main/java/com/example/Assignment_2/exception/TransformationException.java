package com.example.Assignment_2.exception;

/**
 * Exception thrown during transformation of records.
 */
public class TransformationException extends FanOutException {
    public TransformationException(String message) {
        super(message);
    }
    
    public TransformationException(String message, Throwable cause) {
        super(message, cause);
    }
}
