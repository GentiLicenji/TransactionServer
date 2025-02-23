package com.sisal.transaction.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisal.transaction.server.model.rest.ErrorResponse;
import com.sisal.transaction.server.model.db.AccountEntity;
import com.sisal.transaction.server.model.rest.TransactionRequest;
import com.sisal.transaction.server.service.AccountApiService;
import com.sisal.transaction.server.util.ErrorCode;
import com.sisal.transaction.server.util.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test Suite will test the Transaction API controller by ignoring the filter chain.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
@ActiveProfiles("test") // Loads application-test.properties
public class TransactionAPIControllerTest {

    private static final String PATH = "/api/transactions";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountApiService accountAPIService;


    @Test
    public void whenCreateValidTransaction_thenReturnSuccess() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest()
                .accountNumber("GB29NWBK60161331926819")
                .transactionType(TransactionRequest.TransactionTypeEnum.DEPOSIT)
                .amount(1000.0);

        // Act & Assert
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.accountNumber").value("GB29NWBK60161331926819"))
                .andExpect(jsonPath("$.amount").value(1000.0));
    }

    @Test
    public void whenCreateTransactionWithInvalidAmount_thenReturnBadRequest() throws Exception {

        TransactionRequest request = new TransactionRequest()
                // no need for a real account
                // because call should fail before hitting the controller
                .accountNumber("89123949871234879234897")
                .transactionType(TransactionRequest.TransactionTypeEnum.DEPOSIT)
                .amount(11000.0);// Exceeds maximum

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").exists())
                .andExpect(jsonPath("$.errorMessage").exists());
    }

    @Test
    void whenAccountNotFound_thenThrowCorrectErrorResponse() throws Exception {
        TransactionRequest request = new TransactionRequest()
                .accountNumber("NONEXISTENT")
                .amount(10d)
                .transactionType(TransactionRequest.TransactionTypeEnum.DEPOSIT);

        MvcResult result = mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertAll(
                () -> assertEquals("404", errorResponse.getHttpErrorCode()),
                () -> assertEquals(ErrorCode.ACCOUNT_NOT_FOUND.getCode(),
                        errorResponse.getErrorCode()),
                () -> assertNotNull(errorResponse.getErrorMessage())
        );
    }

    @Test
    void whenRateLimitExceeded_thenReturnCorrectErrorResponse() throws Exception {
        // First create an account
        AccountEntity account = createTestAccount();

        // Perform 6 transactions rapidly
        for (int i = 0; i < 6; i++) {

            TransactionRequest request = new TransactionRequest()
                    .accountNumber(account.getAccountNumber())
                    .amount(10.0)
                    .transactionType(TransactionRequest.TransactionTypeEnum.DEPOSIT);

            MvcResult result = mockMvc.perform(post(PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn();

            if (i == 5) { // The 6th transaction should fail
                ErrorResponse errorResponse = objectMapper.readValue(
                        result.getResponse().getContentAsString(),
                        ErrorResponse.class
                );

                assertAll(
                        () -> assertEquals("429", errorResponse.getHttpErrorCode()),
                        () -> assertEquals(ErrorCode.RATE_LIMIT_EXCEEDED.getCode(),
                                errorResponse.getErrorCode()),
                        () -> assertNotNull(errorResponse.getErrorMessage())
                );
            }
        }
    }

    @Test
    void whenInsufficientBalance_thenThrowCorrectErrorResponse() throws Exception {

        AccountEntity account = createTestAccount();

        TransactionRequest request = new TransactionRequest()
                .accountNumber(account.getAccountNumber())
                .amount(200.0)
                .transactionType(TransactionRequest.TransactionTypeEnum.WITHDRAWAL);

        MvcResult result = mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertAll(
                () -> assertEquals("400", errorResponse.getHttpErrorCode()),
                () -> assertEquals(ErrorCode.INSUFFICIENT_BALANCE.getCode(),
                        errorResponse.getErrorCode()),
                () -> assertNotNull(errorResponse.getErrorMessage())
        );
    }

    @Test
    void whenNewAccountHasBalanceBelow100_thenReturnCorrectResponse() throws Exception {
        // Create an account with minimum balance
        AccountEntity account = createTestAccount();

        TransactionRequest request = new TransactionRequest()
                .accountNumber(account.getAccountNumber())
                .amount(200.0)
                .transactionType(TransactionRequest.TransactionTypeEnum.WITHDRAWAL);

        MvcResult result = mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertAll(
                () -> assertEquals("400", errorResponse.getHttpErrorCode()),
                () -> assertEquals(ErrorCode.INSUFFICIENT_BALANCE.getCode(),
                        errorResponse.getErrorCode()),
                () -> assertNotNull(errorResponse.getErrorMessage())
        );
    }

    @Test
    void whenOldAccountHasBalanceBelow100_thenThrowErrorResponse() throws Exception {
        // Create an account with minimum balance
        AccountEntity account = createTestAccount();

        TransactionRequest request = new TransactionRequest()
                .accountNumber(account.getAccountNumber())
                .amount(200.0)
                .transactionType(TransactionRequest.TransactionTypeEnum.WITHDRAWAL);

        MvcResult result = mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertAll(
                () -> assertEquals("400", errorResponse.getHttpErrorCode()),
                () -> assertEquals(ErrorCode.INSUFFICIENT_BALANCE.getCode(),
                        errorResponse.getErrorCode()),
                () -> assertEquals("Balance cannot drop below $100 for existing accounts", errorResponse.getErrorMessage())
        );
    }

    /**
     * Helper method to create a test account with minimum balance.
     *
     * @return random bank account
     */
    private AccountEntity createTestAccount() {

        return accountAPIService.createAccount(
                3453454358L,
                "Bob",
                "Builder",
                "ACC" + Math.random(),
                100.0
        );
    }

}