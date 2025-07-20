package com.assignment.eaglebank.service;

import com.assignment.eaglebank.entity.AccountEntity;
import com.assignment.eaglebank.entity.TransactionEntity;
import com.assignment.eaglebank.entity.TransactionType;
import com.assignment.eaglebank.exception.ResourceNotFoundException;
import com.assignment.eaglebank.exception.AccessDeniedException;
import com.assignment.eaglebank.exception.InsufficientBalanceException;
import com.assignment.eaglebank.model.*;
import com.assignment.eaglebank.repository.AccountRepository;
import com.assignment.eaglebank.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for payment and transaction-related business operations.
 */
@Service
@Transactional
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    /**
     * Processes a new transaction for the specified account.
     */
    public TransactionResponse processTransaction(String userId, String accountNumber, CreateTransactionRequest request) {
        logger.info("Processing transaction for account {} by user: {}", accountNumber, userId);
        
        // Find and verify account ownership
        AccountEntity account = validateAccountOwnership(userId, accountNumber);
        
        // Validate transaction amount
        BigDecimal amount = BigDecimal.valueOf(request.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }
        
        // Check for sufficient funds if it's a withdrawal
        TransactionType transactionType = TransactionType.valueOf(request.getType().getValue().toUpperCase());
        if (transactionType == TransactionType.WITHDRAWAL) {
            if (!account.hasSufficientBalance(amount)) {
                throw new InsufficientBalanceException(account.getBalance(), amount);
            }
        }
        
        // Create transaction entity
        TransactionEntity transaction = new TransactionEntity();
        transaction.setId(generateTransactionReference());
        transaction.setAccount(account);
        transaction.setAmount(amount);
        transaction.setCurrency(request.getCurrency().getValue());
        transaction.setType(transactionType);
        transaction.setReference(request.getReference());
        transaction.setCreatedTimestamp(OffsetDateTime.now());
        
        // Update account balance
        if (transactionType == TransactionType.DEPOSIT) {
            account.credit(amount);
        } else {
            account.debit(amount);
        }
        
        // Save transaction and updated account
        TransactionEntity savedTransaction = transactionRepository.save(transaction);
        accountRepository.save(account);
        
        logger.info("Processed transaction {} for account {}", savedTransaction.getId(), accountNumber);
        return buildTransactionResponse(savedTransaction, userId);
    }
    
    /**
     * Retrieves transaction history for the specified account.
     */
    @Transactional(readOnly = true)
    public ListTransactionsResponse getTransactionHistory(String userId, String accountNumber) {
        logger.info("Getting transaction history for account {} by user: {}", accountNumber, userId);
        
        // Find and verify account ownership
        AccountEntity account = validateAccountOwnership(userId, accountNumber);
        
        List<TransactionEntity> transactions = transactionRepository.findByAccountNumberOrderByCreatedTimestampDesc(account.getAccountNumber());
        
        List<TransactionResponse> transactionResponses = transactions.stream()
            .map(transaction -> buildTransactionResponse(transaction, userId))
            .collect(Collectors.toList());
        
        ListTransactionsResponse response = new ListTransactionsResponse();
        response.setTransactions(transactionResponses);
        
        return response;
    }
    
    /**
     * Retrieves a specific transaction by ID.
     */
    @Transactional(readOnly = true)
    public TransactionResponse retrieveTransaction(String userId, String accountNumber, String transactionId) {
        logger.info("Retrieving transaction {} for account {} by user: {}", transactionId, accountNumber, userId);
        
        // Validate account ownership first
        validateAccountOwnership(userId, accountNumber);
        
        // Find transaction and verify it belongs to the user
        TransactionEntity transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));
        
        return buildTransactionResponse(transaction, userId);
    }
    
    /**
     * Helper method to validate account ownership.
     */
    private AccountEntity validateAccountOwnership(String userId, String accountNumber) {
        // Convert formatted account number (01XXXXXX) to actual Long ID
        Long accountId;
        try {
            // Remove "01" prefix and convert to Long
            accountId = Long.parseLong(accountNumber.substring(2));
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            throw new ResourceNotFoundException("Invalid account number format: " + accountNumber);
        }
        
        // Find account by ID
        AccountEntity account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));
        
        // Verify ownership
        if (!account.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to access this account");
        }
        
        return account;
    }
    
    /**
     * Generates a unique transaction reference ID.
     */
    private String generateTransactionReference() {
        String transactionId;
        do {
            // Generate ID in format "tan-XXXXXX" where X is alphanumeric
            String suffix = UUID.randomUUID().toString().substring(0, 6).replace("-", "");
            transactionId = "tan-" + suffix;
        } while (transactionRepository.existsById(transactionId));
        
        return transactionId;
    }
    
    /**
     * Builds TransactionEntity to TransactionResponse.
     */
    private TransactionResponse buildTransactionResponse(TransactionEntity transaction, String userId) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setAmount(transaction.getAmount().doubleValue());
        response.setCurrency(TransactionResponse.CurrencyEnum.fromValue(transaction.getCurrency()));
        response.setType(TransactionResponse.TypeEnum.fromValue(transaction.getType().name().toLowerCase()));
        response.setReference(transaction.getReference());
        response.setUserId(userId);
        response.setCreatedTimestamp(transaction.getCreatedTimestamp());
        return response;
    }
} 