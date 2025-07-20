package com.assignment.eaglebank.util;

import com.assignment.eaglebank.entity.AccountEntity;
import com.assignment.eaglebank.entity.TransactionEntity;
import com.assignment.eaglebank.entity.TransactionType;
import com.assignment.eaglebank.entity.UserEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Utility class for building test data objects
 */
public class TestDataBuilder {

    // Simple Login DTO for testing
    public static class LoginTestRequest {
        private String email;
        private String password;

        public LoginTestRequest() {}

        public LoginTestRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class UserEntityBuilder {
        private UserEntity user = new UserEntity();

        public UserEntityBuilder() {
            user.setId("usr-" + UUID.randomUUID().toString().replace("-", ""));
            user.setName("Test User");
            user.setEmail("test@example.com");
            user.setPhoneNumber("+1234567890");
            user.setPasswordHash("$2a$10$hashedPassword");
            user.setAddressLine1("123 Test Street");
            user.setTown("Test City");
            user.setCounty("Test County");
            user.setPostcode("TS1 1AA");
            user.setDeleted(false);
            user.setCreatedTimestamp(OffsetDateTime.now());
            user.setUpdatedTimestamp(OffsetDateTime.now());
        }

        public UserEntityBuilder withId(String id) {
            user.setId(id);
            return this;
        }

        public UserEntityBuilder withEmail(String email) {
            user.setEmail(email);
            return this;
        }

        public UserEntityBuilder withName(String name) {
            user.setName(name);
            return this;
        }

        public UserEntityBuilder withPasswordHash(String passwordHash) {
            user.setPasswordHash(passwordHash);
            return this;
        }

        public UserEntity build() {
            return user;
        }
    }

    public static class AccountEntityBuilder {
        private AccountEntity account = new AccountEntity();
        private UserEntity defaultUser;

        public AccountEntityBuilder() {
            // Create a default user for the account
            defaultUser = userEntity().withId("usr-test123").build();
            
            account.setAccountNumber(1L);
            account.setName("Test Account");
            account.setAccountType("personal");
            account.setCurrency("GBP");
            account.setBalance(BigDecimal.valueOf(1000.00));
            account.setSortCode("10-10-10");
            account.setUser(defaultUser);
            account.setCreatedTimestamp(OffsetDateTime.now());
            account.setUpdatedTimestamp(OffsetDateTime.now());
        }

        public AccountEntityBuilder withAccountNumber(Long accountNumber) {
            account.setAccountNumber(accountNumber);
            return this;
        }

        public AccountEntityBuilder withUser(UserEntity user) {
            account.setUser(user);
            return this;
        }

        public AccountEntityBuilder withBalance(BigDecimal balance) {
            account.setBalance(balance);
            return this;
        }

        public AccountEntityBuilder withName(String name) {
            account.setName(name);
            return this;
        }

        public AccountEntity build() {
            return account;
        }
    }

    public static class TransactionEntityBuilder {
        private TransactionEntity transaction = new TransactionEntity();
        private AccountEntity defaultAccount;

        public TransactionEntityBuilder() {
            // Create a default account for the transaction
            defaultAccount = accountEntity().withAccountNumber(1000001L).build();
            
            transaction.setId("tan-" + UUID.randomUUID().toString().substring(0, 6));
            transaction.setAccount(defaultAccount);
            transaction.setAmount(BigDecimal.valueOf(100.00));
            transaction.setCurrency("GBP");
            transaction.setType(TransactionType.DEPOSIT);
            transaction.setReference("Test transaction");
            transaction.setCreatedTimestamp(OffsetDateTime.now());
        }

        public TransactionEntityBuilder withId(String id) {
            transaction.setId(id);
            return this;
        }

        public TransactionEntityBuilder withAccount(AccountEntity account) {
            transaction.setAccount(account);
            return this;
        }

        public TransactionEntityBuilder withAmount(BigDecimal amount) {
            transaction.setAmount(amount);
            return this;
        }

        public TransactionEntityBuilder withType(TransactionType type) {
            transaction.setType(type);
            return this;
        }

        public TransactionEntityBuilder withReference(String reference) {
            transaction.setReference(reference);
            return this;
        }

        public TransactionEntity build() {
            return transaction;
        }
    }

    public static class LoginTestRequestBuilder {
        private LoginTestRequest request = new LoginTestRequest();

        public LoginTestRequestBuilder() {
            request.setEmail("test@example.com");
            request.setPassword("SecurePassword123");
        }

        public LoginTestRequestBuilder withEmail(String email) {
            request.setEmail(email);
            return this;
        }

        public LoginTestRequestBuilder withPassword(String password) {
            request.setPassword(password);
            return this;
        }

        public LoginTestRequest build() {
            return request;
        }
    }

    // Factory methods
    public static UserEntityBuilder userEntity() {
        return new UserEntityBuilder();
    }

    public static AccountEntityBuilder accountEntity() {
        return new AccountEntityBuilder();
    }

    public static TransactionEntityBuilder transactionEntity() {
        return new TransactionEntityBuilder();
    }

    public static LoginTestRequestBuilder loginRequest() {
        return new LoginTestRequestBuilder();
    }
} 