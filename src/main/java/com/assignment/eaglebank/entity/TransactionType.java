package com.assignment.eaglebank.entity;

/**
 * Enum representing the type of transaction
 */
public enum TransactionType {
    /**
     * Deposit transaction - adds money to account
     */
    DEPOSIT("deposit"),
    
    /**
     * Withdrawal transaction - removes money from account
     */
    WITHDRAWAL("withdrawal");

    private final String value;

    TransactionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TransactionType fromValue(String value) {
        for (TransactionType type : TransactionType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown transaction type: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
} 