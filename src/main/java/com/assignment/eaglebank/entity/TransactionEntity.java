package com.assignment.eaglebank.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Transaction entity representing a bank transaction
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_account", columnList = "account_number"),
    @Index(name = "idx_transaction_created", columnList = "created_timestamp")
})
public class TransactionEntity {

    @Id
    @Column(name = "id", length = 50, nullable = false, updatable = false, unique = true)
    private String id;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "10000.00", message = "Amount cannot exceed £10,000")
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "GBP"; // Only GBP supported as per API spec

    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "reference", length = 255)
    private String reference;

    @NotNull(message = "Account is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_number", nullable = false, foreignKey = @ForeignKey(name = "fk_transaction_account"))
    private AccountEntity account;

    @CreationTimestamp
    @Column(name = "created_timestamp", nullable = false, updatable = false)
    private OffsetDateTime createdTimestamp;

    // Constructors
    public TransactionEntity() {}

    public TransactionEntity(BigDecimal amount, TransactionType type, String reference, AccountEntity account) {
        this.amount = amount;
        this.type = type;
        this.reference = reference;
        this.account = account;
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = "tan-" + UUID.randomUUID().toString().replace("-", "");
        }
    }

    // Business methods
    /**
     * Get the user ID associated with this transaction
     */
    @Transient
    public String getUserId() {
        return account != null && account.getUser() != null ? account.getUser().getId() : null;
    }

    /**
     * Check if this is a deposit transaction
     */
    public boolean isDeposit() {
        return TransactionType.DEPOSIT.equals(type);
    }

    /**
     * Check if this is a withdrawal transaction
     */
    public boolean isWithdrawal() {
        return TransactionType.WITHDRAWAL.equals(type);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public AccountEntity getAccount() {
        return account;
    }

    public void setAccount(AccountEntity account) {
        this.account = account;
    }

    public OffsetDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(OffsetDateTime createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    @Override
    public String toString() {
        return "TransactionEntity{" +
                "id='" + id + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", type=" + type +
                ", reference='" + reference + '\'' +
                ", accountNumber=" + (account != null ? account.getFormattedAccountNumber() : null) +
                ", createdTimestamp=" + createdTimestamp +
                '}';
    }
} 