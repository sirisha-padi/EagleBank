package com.assignment.eaglebank.controller;

import com.assignment.eaglebank.api.TransactionApi;
import com.assignment.eaglebank.model.CreateTransactionRequest;
import com.assignment.eaglebank.model.ListTransactionsResponse;
import com.assignment.eaglebank.model.TransactionResponse;
import com.assignment.eaglebank.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;


/**
 * Controller for transaction management operations
 */
@RestController
public class TransactionController implements TransactionApi {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    private final PaymentService paymentService;

    public TransactionController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public ResponseEntity<TransactionResponse> createTransaction(String accountNumber, 
                                                               CreateTransactionRequest createTransactionRequest) {
        logger.info("Creating transaction for account: {}", accountNumber);
        
        String authenticatedUserId = getAuthenticatedUserId();
        TransactionResponse response = paymentService.processTransaction(authenticatedUserId, accountNumber, createTransactionRequest);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<ListTransactionsResponse> listAccountTransaction(String accountNumber) {
        logger.debug("Listing transactions for account: {}", accountNumber);
        
        String authenticatedUserId = getAuthenticatedUserId();
        ListTransactionsResponse response = paymentService.getTransactionHistory(authenticatedUserId, accountNumber);
        
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TransactionResponse> fetchAccountTransactionByID(String accountNumber, String transactionId) {
        logger.debug("Fetching transaction {} for account: {}", transactionId, accountNumber);
        
        String authenticatedUserId = getAuthenticatedUserId();
        TransactionResponse response = paymentService.retrieveTransaction(authenticatedUserId, accountNumber, transactionId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get the authenticated user ID from the security context
     */
    private String getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        
        return (String) authentication.getPrincipal();
    }
} 