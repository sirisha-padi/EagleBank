package com.assignment.eaglebank.exception;

/**
 * Base exception class for banking application.
 * All custom exceptions should extend this class.
 */
public class ApplicationException extends RuntimeException {
    
    public ApplicationException(String message) {
        super(message);
    }
    
    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
} 