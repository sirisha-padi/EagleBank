package com.assignment.eaglebank.service;

import com.assignment.eaglebank.entity.AccountEntity;
import com.assignment.eaglebank.entity.TransactionEntity;
import com.assignment.eaglebank.entity.TransactionType;
import com.assignment.eaglebank.entity.UserEntity;
import com.assignment.eaglebank.exception.ResourceNotFoundException;
import com.assignment.eaglebank.exception.AccessDeniedException;
import com.assignment.eaglebank.exception.InsufficientBalanceException;
import com.assignment.eaglebank.model.CreateTransactionRequest;
import com.assignment.eaglebank.model.ListTransactionsResponse;
import com.assignment.eaglebank.model.TransactionResponse;
import com.assignment.eaglebank.repository.AccountRepository;
import com.assignment.eaglebank.repository.TransactionRepository;
import com.assignment.eaglebank.util.TestDataBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PaymentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private PaymentService paymentService;

    private UserEntity testUser;
    private AccountEntity testAccount;
    private TransactionEntity testTransaction;

    @BeforeEach
    void setUp() {
        testUser = TestDataBuilder.userEntity()
                .withId("usr-test123")
                .withEmail("test@example.com")
                .withName("Test User")
                .build();

        testAccount = TestDataBuilder.accountEntity()
                .withAccountNumber(1L)
                .withUser(testUser)
                .withName("Test Account")
                .withBalance(BigDecimal.valueOf(1000.00))
                .build();

        testTransaction = TestDataBuilder.transactionEntity()
                .withId("tan-abc123")
                .withAccount(testAccount)
                .withAmount(BigDecimal.valueOf(100.00))
                .withType(TransactionType.DEPOSIT)
                .withReference("Test transaction")
                .build();
    }

    // ==================== processTransaction Tests ====================

    @Test
    void processTransaction_Deposit_Success() {
        // Given
        String userId = "usr-test123";
        String accountNumber = "01000001";
        CreateTransactionRequest request = new CreateTransactionRequest()
                .amount(200.0)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.DEPOSIT)
                .reference("Salary deposit");

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.existsById(anyString())).thenReturn(false);
        when(transactionRepository.save(any(TransactionEntity.class))).thenReturn(testTransaction);
        when(accountRepository.save(any(AccountEntity.class))).thenReturn(testAccount);

        // When
        TransactionResponse result = paymentService.processTransaction(userId, accountNumber, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(100.0); // From mocked testTransaction
        assertThat(result.getType()).isEqualTo(TransactionResponse.TypeEnum.DEPOSIT);
        assertThat(result.getUserId()).isEqualTo(userId);

        verify(accountRepository).findById(1L);
        verify(transactionRepository).save(any(TransactionEntity.class));
        verify(accountRepository).save(any(AccountEntity.class));
    }

    @Test
    void processTransaction_Withdrawal_Success() {
        // Given
        String userId = "usr-test123";
        String accountNumber = "01000001";
        CreateTransactionRequest request = new CreateTransactionRequest()
                .amount(300.0)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.WITHDRAWAL)
                .reference("ATM withdrawal");

        TransactionEntity withdrawalTransaction = TestDataBuilder.transactionEntity()
                .withId("tan-withdrawal")
                .withAccount(testAccount)
                .withAmount(BigDecimal.valueOf(300.00))
                .withType(TransactionType.WITHDRAWAL)
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.existsById(anyString())).thenReturn(false);
        when(transactionRepository.save(any(TransactionEntity.class))).thenReturn(withdrawalTransaction);
        when(accountRepository.save(any(AccountEntity.class))).thenReturn(testAccount);

        // When
        TransactionResponse result = paymentService.processTransaction(userId, accountNumber, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(300.0);
        assertThat(result.getType()).isEqualTo(TransactionResponse.TypeEnum.WITHDRAWAL);
        assertThat(result.getUserId()).isEqualTo(userId);

        verify(accountRepository).findById(1L);
        verify(transactionRepository).save(any(TransactionEntity.class));
        verify(accountRepository).save(any(AccountEntity.class));
    }

    @Test
    void processTransaction_InsufficientFunds_ThrowsException() {
        // Given
        String userId = "usr-test123";
        String accountNumber = "01000001";
        CreateTransactionRequest request = new CreateTransactionRequest()
                .amount(2000.0) // More than account balance of 1000
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.WITHDRAWAL)
                .reference("Large withdrawal");

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> paymentService.processTransaction(userId, accountNumber, request))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(accountRepository).findById(1L);
        verify(transactionRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void processTransaction_AccountNotFound_ThrowsException() {
        // Given
        String userId = "usr-test123";
        String accountNumber = "01000001";
        CreateTransactionRequest request = new CreateTransactionRequest()
                .amount(100.0)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.DEPOSIT);

        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.processTransaction(userId, accountNumber, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found: 01000001");

        verify(accountRepository).findById(1L);
    }

    @Test
    void processTransaction_AccessDenied_ThrowsException() {
        // Given
        String userId = "usr-different";
        String accountNumber = "01000001";
        CreateTransactionRequest request = new CreateTransactionRequest()
                .amount(100.0)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.DEPOSIT);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> paymentService.processTransaction(userId, accountNumber, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You don't have permission to access this account");

        verify(accountRepository).findById(1L);
    }

    @Test
    void processTransaction_InvalidAmount_ThrowsException() {
        // Given
        String userId = "usr-test123";
        String accountNumber = "01000001";
        CreateTransactionRequest request = new CreateTransactionRequest()
                .amount(0.0) // Invalid amount
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.DEPOSIT);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> paymentService.processTransaction(userId, accountNumber, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transaction amount must be positive");

        verify(accountRepository).findById(1L);
    }

    // ==================== getTransactionHistory Tests ====================

    @Test
    void getTransactionHistory_Success() {
        // Given
        String userId = "usr-test123";
        String accountNumber = "01000001";
        
        List<TransactionEntity> transactions = Arrays.asList(testTransaction);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccountNumberOrderByCreatedTimestampDesc(1L))
                .thenReturn(transactions);

        // When
        ListTransactionsResponse result = paymentService.getTransactionHistory(userId, accountNumber);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTransactions()).hasSize(1);
        assertThat(result.getTransactions().get(0).getId()).isEqualTo("tan-abc123");
        assertThat(result.getTransactions().get(0).getUserId()).isEqualTo(userId);

        verify(accountRepository).findById(1L);
        verify(transactionRepository).findByAccountNumberOrderByCreatedTimestampDesc(1L);
    }

    @Test
    void getTransactionHistory_EmptyHistory() {
        // Given
        String userId = "usr-test123";
        String accountNumber = "01000001";

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccountNumberOrderByCreatedTimestampDesc(1L))
                .thenReturn(Collections.emptyList());

        // When
        ListTransactionsResponse result = paymentService.getTransactionHistory(userId, accountNumber);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTransactions()).isEmpty();

        verify(accountRepository).findById(1L);
        verify(transactionRepository).findByAccountNumberOrderByCreatedTimestampDesc(1L);
    }

    @Test
    void getTransactionHistory_AccountNotFound_ThrowsException() {
        // Given
        String userId = "usr-test123";
        String accountNumber = "01000001";

        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.getTransactionHistory(userId, accountNumber))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(accountRepository).findById(1L);
        verify(transactionRepository, never()).findByAccountNumberOrderByCreatedTimestampDesc(anyLong());
    }

    @Test
    void getTransactionHistory_AccessDenied_ThrowsException() {
        // Given
        String userId = "usr-different";
        String accountNumber = "01000001";

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> paymentService.getTransactionHistory(userId, accountNumber))
                .isInstanceOf(AccessDeniedException.class);

        verify(accountRepository).findById(1L);
    }

    // ==================== retrieveTransaction Tests ====================

    @Test
    void retrieveTransaction_Success() {
        // Given
        String userId = "usr-test123";
        String accountNumber = "01000001";
        String transactionId = "tan-abc123";

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByIdAndUserId(transactionId, userId))
                .thenReturn(Optional.of(testTransaction));

        // When
        TransactionResponse result = paymentService.retrieveTransaction(userId, accountNumber, transactionId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(transactionId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualTo(100.0);

        verify(accountRepository).findById(1L);
        verify(transactionRepository).findByIdAndUserId(transactionId, userId);
    }

    @Test
    void retrieveTransaction_TransactionNotFound_ThrowsException() {
        // Given
        String userId = "usr-test123";
        String accountNumber = "01000001";
        String transactionId = "tan-nonexistent";

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByIdAndUserId(transactionId, userId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.retrieveTransaction(userId, accountNumber, transactionId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found with ID: " + transactionId);

        verify(accountRepository).findById(1L);
        verify(transactionRepository).findByIdAndUserId(transactionId, userId);
    }

    @Test
    void retrieveTransaction_AccountNotFound_ThrowsException() {
        // Given
        String userId = "usr-test123";
        String accountNumber = "01000001";
        String transactionId = "tan-abc123";

        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.retrieveTransaction(userId, accountNumber, transactionId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(accountRepository).findById(1L);
        verify(transactionRepository, never()).findByIdAndUserId(anyString(), anyString());
    }

    @Test
    void retrieveTransaction_InvalidAccountNumber_ThrowsException() {
        // Given
        String userId = "usr-test123";
        String accountNumber = "invalid";
        String transactionId = "tan-abc123";

        // When & Then
        assertThatThrownBy(() -> paymentService.retrieveTransaction(userId, accountNumber, transactionId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invalid account number format");
    }
} 