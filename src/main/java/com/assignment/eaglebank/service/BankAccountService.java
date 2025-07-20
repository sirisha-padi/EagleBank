package com.assignment.eaglebank.service;

import com.assignment.eaglebank.entity.AccountEntity;
import com.assignment.eaglebank.entity.UserEntity;
import com.assignment.eaglebank.exception.ResourceNotFoundException;
import com.assignment.eaglebank.exception.AccessDeniedException;
import com.assignment.eaglebank.exception.BusinessRuleViolationException;
import com.assignment.eaglebank.repository.AccountRepository;
import com.assignment.eaglebank.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

// Import generated OpenAPI models
import com.assignment.eaglebank.model.BankAccountResponse;
import com.assignment.eaglebank.model.CreateBankAccountRequest;
import com.assignment.eaglebank.model.UpdateBankAccountRequest;
import com.assignment.eaglebank.model.ListBankAccountsResponse;

/**
 * Service class for bank account-related business operations.
 */
@Service
@Transactional
public class BankAccountService {
    
    private static final Logger logger = LoggerFactory.getLogger(BankAccountService.class);
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Opens a new bank account for the authenticated user.
     */
    public BankAccountResponse openAccount(String userId, CreateBankAccountRequest request) {
        logger.info("Opening account for user: {}", userId);
        
        // Verify user exists
        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        // Create account entity
        AccountEntity account = new AccountEntity();
        account.setUser(user);
        account.setName(request.getName());
        account.setAccountType(request.getAccountType().getValue());
        
        AccountEntity savedAccount = accountRepository.save(account);
        logger.info("Opened account with number: {}", savedAccount.getFormattedAccountNumber());
        
        return convertToResponse(savedAccount);
    }
    
    /**
     * Retrieves all accounts for the authenticated user.
     */
    @Transactional(readOnly = true)
    public ListBankAccountsResponse retrieveAccountList(String userId) {
        logger.info("Retrieving accounts for user: {}", userId);
        
        // Verify user exists
        userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        List<AccountEntity> accounts = accountRepository.findByUserId(userId);
        
        List<BankAccountResponse> accountResponses = accounts.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        ListBankAccountsResponse response = new ListBankAccountsResponse();
        response.setAccounts(accountResponses);
        
        return response;
    }
    
    /**
     * Gets a specific account by account number.
     */
    @Transactional(readOnly = true)
    public BankAccountResponse getAccount(String userId, String accountNumber) {
        logger.info("Getting account {} for user: {}", accountNumber, userId);
        
        AccountEntity account = validateAccountAccess(userId, accountNumber);
        return convertToResponse(account);
    }
    
    /**
     * Modifies an existing account.
     */
    public BankAccountResponse modifyAccount(String userId, String accountNumber, UpdateBankAccountRequest request) {
        logger.info("Modifying account {} for user: {}", accountNumber, userId);
        
        AccountEntity account = validateAccountAccess(userId, accountNumber);
        
        // Update fields if provided
        if (request.getName() != null) {
            account.setName(request.getName());
        }
        if (request.getAccountType() != null) {
            account.setAccountType(request.getAccountType().getValue());
        }
        
        AccountEntity updatedAccount = accountRepository.save(account);
        
        logger.info("Modified account: {}", accountNumber);
        return convertToResponse(updatedAccount);
    }
    
    /**
     * Closes an account.
     */
    public void closeAccount(String userId, String accountNumber) {
        logger.info("Closing account {} for user: {}", accountNumber, userId);
        
        AccountEntity account = validateAccountAccess(userId, accountNumber);
        
        // Check if account has transactions
        if (account.getTransactions() != null && !account.getTransactions().isEmpty()) {
            throw new BusinessRuleViolationException("Cannot close account with existing transactions");
        }
        
        // Check if account has non-zero balance
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessRuleViolationException("Cannot close account with non-zero balance");
        }
        
        accountRepository.delete(account);
        logger.info("Closed account: {}", accountNumber);
    }
    
    /**
     * Helper method to validate account access and verify user ownership.
     */
    private AccountEntity validateAccountAccess(String userId, String accountNumber) {
        AccountEntity account = accountRepository.findByFormattedAccountNumber(accountNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Account", accountNumber));
        
        if (!account.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to access this account");
        }
        
        return account;
    }
    
    /**
     * Converts AccountEntity to BankAccountResponse.
     */
    private BankAccountResponse convertToResponse(AccountEntity account) {
        BankAccountResponse response = new BankAccountResponse();
        response.setAccountNumber(account.getFormattedAccountNumber());
        response.setSortCode(BankAccountResponse.SortCodeEnum.fromValue(account.getSortCode()));
        response.setName(account.getName());
        response.setAccountType(BankAccountResponse.AccountTypeEnum.fromValue(account.getAccountType()));
        response.setBalance(account.getBalance().doubleValue());
        response.setCurrency(BankAccountResponse.CurrencyEnum.fromValue(account.getCurrency()));
        response.setCreatedTimestamp(account.getCreatedTimestamp());
        response.setUpdatedTimestamp(account.getUpdatedTimestamp());
        return response;
    }
} 