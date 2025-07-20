package com.assignment.eaglebank.exception;

/**
 * Exception thrown when a user tries to access a resource they don't have permission for.
 */
public class AccessDeniedException extends ApplicationException {
    
    public AccessDeniedException(String message) {
        super(message);
    }
    
    public AccessDeniedException() {
        super("Access denied: You don't have permission to access this resource");
    }
} 