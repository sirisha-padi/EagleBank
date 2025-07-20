package com.assignment.eaglebank.controller;

import com.assignment.eaglebank.api.AccountApi;
import com.assignment.eaglebank.model.BankAccountResponse;
import com.assignment.eaglebank.model.CreateBankAccountRequest;
import com.assignment.eaglebank.model.ListBankAccountsResponse;
import com.assignment.eaglebank.model.UpdateBankAccountRequest;
import com.assignment.eaglebank.service.BankAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;


/**
 * Controller for account management operations
 */
@RestController
public class AccountController implements AccountApi {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private final BankAccountService bankAccountService;

    public AccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @Override
    public ResponseEntity<BankAccountResponse> createAccount(CreateBankAccountRequest createBankAccountRequest) {
        logger.info("Creating new account");
        
        String authenticatedUserId = getAuthenticatedUserId();
        BankAccountResponse response = bankAccountService.openAccount(authenticatedUserId, createBankAccountRequest);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<ListBankAccountsResponse> listAccounts() {
        logger.debug("Listing accounts");
        
        String authenticatedUserId = getAuthenticatedUserId();
        ListBankAccountsResponse response = bankAccountService.retrieveAccountList(authenticatedUserId);
        
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<BankAccountResponse> fetchAccountByAccountNumber(String accountNumber) {
        logger.debug("Fetching account with number: {}", accountNumber);
        
        String authenticatedUserId = getAuthenticatedUserId();
        BankAccountResponse response = bankAccountService.getAccount(authenticatedUserId, accountNumber);
        
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<BankAccountResponse> updateAccountByAccountNumber(String accountNumber, 
                                                                           UpdateBankAccountRequest updateBankAccountRequest) {
        logger.info("Updating account with number: {}", accountNumber);
        
        String authenticatedUserId = getAuthenticatedUserId();
        BankAccountResponse response = bankAccountService.modifyAccount(authenticatedUserId, accountNumber, updateBankAccountRequest);
        
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> deleteAccountByAccountNumber(String accountNumber) {
        logger.info("Deleting account with number: {}", accountNumber);
        
        String authenticatedUserId = getAuthenticatedUserId();
        bankAccountService.closeAccount(authenticatedUserId, accountNumber);
        
        return ResponseEntity.noContent().build();
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