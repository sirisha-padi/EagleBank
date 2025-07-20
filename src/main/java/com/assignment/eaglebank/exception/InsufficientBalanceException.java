package com.assignment.eaglebank.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when a transaction cannot be completed due to insufficient balance.
 */
public class InsufficientBalanceException extends ApplicationException {
    
    public InsufficientBalanceException(String message) {
        super(message);
    }
    
    public InsufficientBalanceException(BigDecimal currentBalance, BigDecimal requestedAmount) {
        super(String.format("Insufficient balance: Current balance %.2f, Requested amount %.2f", 
              currentBalance, requestedAmount));
    }
} 