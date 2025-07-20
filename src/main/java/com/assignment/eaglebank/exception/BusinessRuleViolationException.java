package com.assignment.eaglebank.exception;

/**
 * Exception thrown when there's a violation of business rules.
 * For example, trying to delete a user who has active accounts.
 */
public class BusinessRuleViolationException extends ApplicationException {
    
    public BusinessRuleViolationException(String message) {
        super(message);
    }
} 