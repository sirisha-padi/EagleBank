package com.assignment.eaglebank.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the Eagle Bank application.
 * Tests are ordered to create dependencies between tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EagleBankIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String jwtToken;
    private static String userId;
    private static String accountNumber;
    private static String transactionId;

    @Test
    @Order(1)
    void createUser_Success() throws Exception {
        String userJson = """
            {
                "name": "John Doe",
                "email": "john.doe@test.com",
                "phoneNumber": "+447123456789",
                "password": "MySecurePassword123",
                "address": {
                    "line1": "123 Main Street",
                    "town": "London",
                    "county": "Greater London",
                    "postcode": "SW1A 1AA"
                }
            }
            """;

        MvcResult result = mockMvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@test.com"))
                .andExpect(jsonPath("$.phoneNumber").value("+447123456789"))
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        userId = objectMapper.readTree(responseContent).get("id").asText();
    }

    @Test
    @Order(2)
    void createUser_DuplicateEmail_ReturnsConflict() throws Exception {
        String userJson = """
            {
                "name": "Jane Doe",
                "email": "john.doe@test.com",
                "phoneNumber": "+447123456788",
                "password": "AnotherPassword123",
                "address": {
                    "line1": "456 Other Street",
                    "town": "Manchester",
                    "county": "Greater Manchester",
                    "postcode": "M1 1AA"
                }
            }
            """;

        mockMvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    @Order(3)
    void createUser_InvalidPhoneNumber_ReturnsBadRequest() throws Exception {
        String userJson = """
            {
                "name": "Invalid User",
                "email": "invalid@test.com",
                "phoneNumber": "invalid-phone",
                "password": "Password123",
                "address": {
                    "line1": "123 Test Street",
                    "town": "Test Town",
                    "county": "Test County",
                    "postcode": "TE1 1ST"
                }
            }
            """;

        mockMvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid phone number format")));
    }

    @Test
    @Order(4)
    void authenticateUser_Success() throws Exception {
        String authJson = """
            {
                "email": "john.doe@test.com",
                "password": "MySecurePassword123"
            }
            """;

        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(authJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.userId").value(userId))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        jwtToken = objectMapper.readTree(responseContent).get("token").asText();
    }

    @Test
    @Order(5)
    void authenticateUser_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        String authJson = """
            {
                "email": "john.doe@test.com",
                "password": "WrongPassword"
            }
            """;

        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(authJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    void getUser_Success() throws Exception {
        mockMvc.perform(get("/v1/users/" + userId)
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@test.com"));
    }

    @Test
    @Order(7)
    void getUser_Unauthorized_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/v1/users/" + userId))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    void createAccount_Success() throws Exception {
        String accountJson = """
            {
                "name": "My Savings Account",
                "accountType": "personal"
            }
            """;

        MvcResult result = mockMvc.perform(post("/v1/accounts")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(accountJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Savings Account"))
                .andExpect(jsonPath("$.accountType").value("personal"))
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.currency").value("GBP"))
                .andExpect(jsonPath("$.sortCode").value("10-10-10"))
                .andExpect(jsonPath("$.accountNumber").exists())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        accountNumber = objectMapper.readTree(responseContent).get("accountNumber").asText();
    }

    @Test
    @Order(9)
    void listAccounts_Success() throws Exception {
        mockMvc.perform(get("/v1/accounts")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts").isArray())
                .andExpect(jsonPath("$.accounts[0].accountNumber").value(accountNumber));
    }

    @Test
    @Order(10)
    void getAccount_Success() throws Exception {
        mockMvc.perform(get("/v1/accounts/" + accountNumber)
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(accountNumber))
                .andExpect(jsonPath("$.name").value("My Savings Account"));
    }

    @Test
    @Order(11)
    void createTransaction_Deposit_Success() throws Exception {
        String transactionJson = """
            {
                "amount": 1000.00,
                "currency": "GBP",
                "type": "deposit",
                "reference": "Initial deposit"
            }
            """;

        MvcResult result = mockMvc.perform(post("/v1/accounts/" + accountNumber + "/transactions")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transactionJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(1000.0))
                .andExpect(jsonPath("$.type").value("deposit"))
                .andExpect(jsonPath("$.currency").value("GBP"))
                .andExpect(jsonPath("$.reference").value("Initial deposit"))
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        transactionId = objectMapper.readTree(responseContent).get("id").asText();
    }

    @Test
    @Order(12)
    void createTransaction_Withdrawal_Success() throws Exception {
        String transactionJson = """
            {
                "amount": 200.00,
                "currency": "GBP",
                "type": "withdrawal",
                "reference": "ATM withdrawal"
            }
            """;

        mockMvc.perform(post("/v1/accounts/" + accountNumber + "/transactions")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transactionJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(200.0))
                .andExpect(jsonPath("$.type").value("withdrawal"))
                .andExpect(jsonPath("$.currency").value("GBP"));
    }

    @Test
    @Order(13)
    void createTransaction_InsufficientFunds_ReturnsUnprocessableEntity() throws Exception {
        String transactionJson = """
            {
                "amount": 5000.00,
                "currency": "GBP",
                "type": "withdrawal"
            }
            """;

        mockMvc.perform(post("/v1/accounts/" + accountNumber + "/transactions")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transactionJson))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Order(14)
    void createTransaction_ExceedsMaxAmount_ReturnsBadRequest() throws Exception {
        String transactionJson = """
            {
                "amount": 15000.00,
                "currency": "GBP",
                "type": "deposit"
            }
            """;

        mockMvc.perform(post("/v1/accounts/" + accountNumber + "/transactions")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transactionJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].message").value(containsString("10000")));
    }

    @Test
    @Order(15)
    void listTransactions_Success() throws Exception {
        mockMvc.perform(get("/v1/accounts/" + accountNumber + "/transactions")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions[0]").exists());
    }

    @Test
    @Order(16)
    void getTransaction_Success() throws Exception {
        mockMvc.perform(get("/v1/accounts/" + accountNumber + "/transactions/" + transactionId)
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId))
                .andExpect(jsonPath("$.amount").value(1000.0));
    }

    @Test
    @Order(17)
    void updateAccount_Success() throws Exception {
        String updateJson = """
            {
                "name": "Updated Savings Account"
            }
            """;

        mockMvc.perform(patch("/v1/accounts/" + accountNumber)
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Savings Account"))
                .andExpect(jsonPath("$.accountNumber").value(accountNumber));
    }

    @Test
    @Order(18)
    void updateUser_Success() throws Exception {
        String updateJson = """
            {
                "name": "John Updated Doe",
                "phoneNumber": "+447123456790"
            }
            """;

        mockMvc.perform(patch("/v1/users/" + userId)
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Updated Doe"))
                .andExpect(jsonPath("$.phoneNumber").value("+447123456790"));
    }

    @Test
    @Order(19)
    void closeAccount_WithBalance_ReturnsConflict() throws Exception {
        // Try to delete account that still has balance and transactions
        mockMvc.perform(delete("/v1/accounts/" + accountNumber)
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(20)
    void invalidJson_ReturnsBadRequest() throws Exception {
        String invalidJson = "{ invalid json }";

        mockMvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(21)
    void swaggerUI_Accessible() throws Exception {
        // Swagger UI typically redirects to the actual UI page
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isFound()) // 302 redirect is expected
                .andExpect(header().string("Location", "/swagger-ui/index.html"));
    }

    @Test
    @Order(22)
    void openApiDocs_Accessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
} 