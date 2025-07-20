package com.assignment.eaglebank.service;

import com.assignment.eaglebank.entity.AccountEntity;
import com.assignment.eaglebank.entity.UserEntity;
import com.assignment.eaglebank.exception.ResourceNotFoundException;
import com.assignment.eaglebank.exception.AccessDeniedException;
import com.assignment.eaglebank.exception.BusinessRuleViolationException;
import com.assignment.eaglebank.model.BankAccountResponse;
import com.assignment.eaglebank.model.CreateBankAccountRequest;
import com.assignment.eaglebank.model.UpdateBankAccountRequest;
import com.assignment.eaglebank.model.ListBankAccountsResponse;
import com.assignment.eaglebank.repository.AccountRepository;
import com.assignment.eaglebank.repository.UserRepository;
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
class BankAccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BankAccountService bankAccountService;

    private UserEntity testUser;
    private AccountEntity testAccount;

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
    }

    @Test
    void openAccount_Success() {
        // Given
        CreateBankAccountRequest request = new CreateBankAccountRequest();
        request.setName("Savings Account");
        request.setAccountType(CreateBankAccountRequest.AccountTypeEnum.PERSONAL);

        when(userRepository.findByIdAndDeletedFalse("usr-test123")).thenReturn(Optional.of(testUser));
        when(accountRepository.save(any(AccountEntity.class))).thenReturn(testAccount);

        // When
        BankAccountResponse result = bankAccountService.openAccount("usr-test123", request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccountNumber()).isEqualTo("01000001");
        assertThat(result.getName()).isEqualTo("Test Account");
        assertThat(result.getBalance()).isEqualTo(1000.00);

        verify(userRepository).findByIdAndDeletedFalse("usr-test123");
        verify(accountRepository).save(any(AccountEntity.class));
    }

    @Test
    void openAccount_UserNotFound_ThrowsResourceNotFoundException() {
        // Given
        CreateBankAccountRequest request = new CreateBankAccountRequest();
        request.setName("Savings Account");
        request.setAccountType(CreateBankAccountRequest.AccountTypeEnum.PERSONAL);

        when(userRepository.findByIdAndDeletedFalse("usr-nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bankAccountService.openAccount("usr-nonexistent", request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository).findByIdAndDeletedFalse("usr-nonexistent");
        verify(accountRepository, never()).save(any(AccountEntity.class));
    }

    @Test
    void retrieveAccountList_Success() {
        // Given
        List<AccountEntity> accounts = Arrays.asList(testAccount);
        when(userRepository.findByIdAndDeletedFalse("usr-test123")).thenReturn(Optional.of(testUser));
        when(accountRepository.findByUserId("usr-test123")).thenReturn(accounts);

        // When
        ListBankAccountsResponse result = bankAccountService.retrieveAccountList("usr-test123");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccounts()).hasSize(1);
        assertThat(result.getAccounts().get(0).getAccountNumber()).isEqualTo("01000001");

        verify(userRepository).findByIdAndDeletedFalse("usr-test123");
        verify(accountRepository).findByUserId("usr-test123");
    }

    @Test
    void retrieveAccountList_EmptyList() {
        // Given
        when(userRepository.findByIdAndDeletedFalse("usr-test123")).thenReturn(Optional.of(testUser));
        when(accountRepository.findByUserId("usr-test123")).thenReturn(Collections.emptyList());

        // When
        ListBankAccountsResponse result = bankAccountService.retrieveAccountList("usr-test123");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccounts()).isEmpty();

        verify(userRepository).findByIdAndDeletedFalse("usr-test123");
        verify(accountRepository).findByUserId("usr-test123");
    }

    @Test
    void retrieveAccountList_UserNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(userRepository.findByIdAndDeletedFalse("usr-nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bankAccountService.retrieveAccountList("usr-nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository).findByIdAndDeletedFalse("usr-nonexistent");
        verify(accountRepository, never()).findByUserId(anyString());
    }

    @Test
    void getAccount_Success() {
        // Given
        when(accountRepository.findByFormattedAccountNumber("01000001")).thenReturn(Optional.of(testAccount));

        // When
        BankAccountResponse result = bankAccountService.getAccount("usr-test123", "01000001");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccountNumber()).isEqualTo("01000001");
        assertThat(result.getName()).isEqualTo("Test Account");

        verify(accountRepository).findByFormattedAccountNumber("01000001");
    }

    @Test
    void getAccount_AccountNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(accountRepository.findByFormattedAccountNumber("01000001")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bankAccountService.getAccount("usr-test123", "01000001"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(accountRepository).findByFormattedAccountNumber("01000001");
    }

    @Test
    void getAccount_AccessDenied_ThrowsAccessDeniedException() {
        // Given
        UserEntity otherUser = TestDataBuilder.userEntity()
                .withId("usr-other")
                .build();
        AccountEntity otherAccount = TestDataBuilder.accountEntity()
                .withUser(otherUser)
                .build();

        when(accountRepository.findByFormattedAccountNumber("01000001")).thenReturn(Optional.of(otherAccount));

        // When & Then
        assertThatThrownBy(() -> bankAccountService.getAccount("usr-test123", "01000001"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You don't have permission to access this account");

        verify(accountRepository).findByFormattedAccountNumber("01000001");
    }

    @Test
    void modifyAccount_Success() {
        // Given
        UpdateBankAccountRequest request = new UpdateBankAccountRequest();
        request.setName("Updated Account Name");

        when(accountRepository.findByFormattedAccountNumber("01000001")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(AccountEntity.class))).thenReturn(testAccount);

        // When
        BankAccountResponse result = bankAccountService.modifyAccount("usr-test123", "01000001", request);

        // Then
        assertThat(result).isNotNull();
        verify(accountRepository).findByFormattedAccountNumber("01000001");
        verify(accountRepository).save(testAccount);
    }

    @Test
    void modifyAccount_AccountNotFound_ThrowsResourceNotFoundException() {
        // Given
        UpdateBankAccountRequest request = new UpdateBankAccountRequest();
        request.setName("Updated Name");

        when(accountRepository.findByFormattedAccountNumber("01000001")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bankAccountService.modifyAccount("usr-test123", "01000001", request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(accountRepository).findByFormattedAccountNumber("01000001");
        verify(accountRepository, never()).save(any(AccountEntity.class));
    }

    @Test
    void closeAccount_Success() {
        // Given
        // Set up account with zero balance and no transactions
        testAccount.setBalance(BigDecimal.ZERO);
        testAccount.setTransactions(Collections.emptyList());

        when(accountRepository.findByFormattedAccountNumber("01000001")).thenReturn(Optional.of(testAccount));

        // When
        bankAccountService.closeAccount("usr-test123", "01000001");

        // Then
        verify(accountRepository).findByFormattedAccountNumber("01000001");
        verify(accountRepository).delete(testAccount);
    }

    @Test
    void closeAccount_HasTransactions_ThrowsBusinessRuleViolationException() {
        // Given
        // Set up account with transactions (non-empty list)
        testAccount.setTransactions(Arrays.asList(
                TestDataBuilder.transactionEntity().build()
        ));

        when(accountRepository.findByFormattedAccountNumber("01000001")).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> bankAccountService.closeAccount("usr-test123", "01000001"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Cannot close account with existing transactions");

        verify(accountRepository).findByFormattedAccountNumber("01000001");
        verify(accountRepository, never()).delete(any(AccountEntity.class));
    }

    @Test
    void closeAccount_NonZeroBalance_ThrowsBusinessRuleViolationException() {
        // Given
        // Set up account with non-zero balance
        testAccount.setBalance(BigDecimal.valueOf(100.00));
        testAccount.setTransactions(Collections.emptyList());

        when(accountRepository.findByFormattedAccountNumber("01000001")).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> bankAccountService.closeAccount("usr-test123", "01000001"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Cannot close account with non-zero balance");

        verify(accountRepository).findByFormattedAccountNumber("01000001");
        verify(accountRepository, never()).delete(any(AccountEntity.class));
    }

    @Test
    void closeAccount_AccountNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(accountRepository.findByFormattedAccountNumber("01000001")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bankAccountService.closeAccount("usr-test123", "01000001"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(accountRepository).findByFormattedAccountNumber("01000001");
        verify(accountRepository, never()).delete(any(AccountEntity.class));
    }

    @Test
    void validateAccountAccess_AccessDenied_ThrowsAccessDeniedException() {
        // Given
        UserEntity otherUser = TestDataBuilder.userEntity()
                .withId("usr-other")
                .build();
        AccountEntity otherAccount = TestDataBuilder.accountEntity()
                .withUser(otherUser)
                .build();

        when(accountRepository.findByFormattedAccountNumber("01000001")).thenReturn(Optional.of(otherAccount));

        // When & Then
        assertThatThrownBy(() -> bankAccountService.getAccount("usr-test123", "01000001"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You don't have permission to access this account");
    }
} 