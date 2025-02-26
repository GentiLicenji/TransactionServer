package com.sisal.transaction.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisal.transaction.server.model.db.AccountEntity;
import com.sisal.transaction.server.model.rest.ErrorResponse;
import com.sisal.transaction.server.model.rest.TransactionRequest;
import com.sisal.transaction.server.model.rest.TransactionResponse;
import com.sisal.transaction.server.service.AccountApiService;
import com.sisal.transaction.server.util.ErrorCode;
import com.sisal.transaction.test.config.TestConfig;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test Suite for Transaction API Controller Endpoints
 *
 * <p>This test suite performs comprehensive integration testing of the transaction processing
 * endpoints, including validation of business rules, error handling, and successful transaction
 * flows.
 * Tests are executed with MockMvc and use an H2 in-memory database.</p>
 *
 * <p>Configuration:</p>
 * <ul>
 *   <li>Uses H2 database configuration from {@link TestConfig}</li>
 *   <li>Runs with 'test' profile loading application-test.properties</li>
 *   <li>Disabled security filters for focused controller testing</li>
 * </ul>
 *
 * <p>Test Categories:</p>
 * <ul>
 *   <li>Positive Scenarios:
 *     <ul>
 *       <li>Successful transaction creation (deposit/withdrawal)</li>
 *       <li>Balance management within limits</li>
 *       <li>Transaction status verification</li>
 *     </ul>
 *   </li>
 *   <li>Negative Scenarios:
 *     <ul>
 *       <li>Invalid transaction amounts</li>
 *       <li>Non-existent accounts</li>
 *       <li>Rate limiting violations</li>
 *       <li>Insufficient balance handling</li>
 *       <li>Minimum balance violations</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @version 1.2
 * @see TransactionRequest
 * @see TransactionResponse
 * @see ErrorResponse
 * @see AccountEntity
 * @see AccountApiService
 * @see TestConfig
 * @since 1.0
 */

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(TestConfig.class)
@ActiveProfiles("test")
public class TransactionAPIControllerIT {

    private static final String PATH = "/api/transactions";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountApiService accountAPIService;
    private AccountEntity randomAccount;

    @BeforeEach
    void setUp() {
        randomAccount = createTestAccount(100.0);
    }

    /**
     * ******************
     * Positive Scenarios
     * ******************
     */
    @Test
    public void whenCreateValidTransaction_thenReturnCorrectResponse() throws Exception {

        TransactionRequest request = new TransactionRequest()
                .accountNumber(randomAccount.getAccountNumber())
                .transactionType(TransactionRequest.TransactionTypeEnum.DEPOSIT)
                .amount(1000.0);

        // Perform REST call & Assert
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(1000.0))
                .andExpect(jsonPath("$.status").value(TransactionResponse.StatusEnum.COMPLETED.toString()))
                .andExpect(jsonPath("$.accountNumber").value(randomAccount.getAccountNumber()))
                .andExpect(jsonPath("$.transactionType").value(request.getTransactionType().toString()))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.transactionId").exists());
    }

    @Test
    void whenNewAccountBalanceGoesBelow100_thenReturnCorrectResponse() throws Exception {

        TransactionRequest request = new TransactionRequest()
                .accountNumber(randomAccount.getAccountNumber())
                .amount(60.0)
                .transactionType(TransactionRequest.TransactionTypeEnum.WITHDRAWAL);

        // Perform REST call & Assert
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(60.0))
                .andExpect(jsonPath("$.status").value(TransactionResponse.StatusEnum.COMPLETED.toString()))
                .andExpect(jsonPath("$.accountNumber").value(randomAccount.getAccountNumber()))
                .andExpect(jsonPath("$.transactionType").value(request.getTransactionType().toString()))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.transactionId").exists());
    }

    /**
     * ******************
     * Negative Scenarios
     * ******************
     */
    @Test
    public void whenCreateTransactionWithInvalidAmount_thenThrowValidErrorResponse() throws Exception {

        TransactionRequest request = new TransactionRequest()
                // no need for a real account, because call should fail before hitting the controller
                .accountNumber("89123949871234879234897")
                .transactionType(TransactionRequest.TransactionTypeEnum.DEPOSIT)
                .amount(11000.0);// Exceeds maximum

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
                () -> assertEquals(HttpStatus.BAD_REQUEST.toString(), errorResponse.getHttpErrorCode()),
                () -> assertEquals(ErrorCode.INVALID_REQUEST.getCode(),
                        errorResponse.getErrorCode()),
                () -> assertNotNull(errorResponse.getErrorMessage())
        );
    }

    @Test
    void whenAccountNotFound_thenThrowValidErrorResponse() throws Exception {

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
                () -> assertEquals(HttpStatus.NOT_FOUND.toString(), errorResponse.getHttpErrorCode()),
                () -> assertEquals(ErrorCode.ACCOUNT_NOT_FOUND.getCode(),
                        errorResponse.getErrorCode()),
                () -> assertNotNull(errorResponse.getErrorMessage())
        );
    }

    @Test
    void whenRateLimitExceeded_thenThrowValidErrorResponse() throws Exception {

        // Perform 6 transactions rapidly
        for (int i = 0; i < 6; i++) {

            TransactionRequest request = new TransactionRequest()
                    .accountNumber(randomAccount.getAccountNumber())
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
                        () -> assertEquals(HttpStatus.TOO_MANY_REQUESTS.toString(), errorResponse.getHttpErrorCode()),
                        () -> assertEquals(ErrorCode.RATE_LIMIT_EXCEEDED.getCode(),
                                errorResponse.getErrorCode()),
                        () -> assertNotNull(errorResponse.getErrorMessage())
                );
            }
        }
    }

    @Test
    void whenInsufficientBalance_thenThrowValidErrorResponse() throws Exception {

        //The Original balance is $100.0
        TransactionRequest request = new TransactionRequest()
                .accountNumber(randomAccount.getAccountNumber())
                .amount(200.0)
                .transactionType(TransactionRequest.TransactionTypeEnum.WITHDRAWAL);

        MvcResult result = mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertAll(
                () -> assertEquals(HttpStatus.CONFLICT.toString(), errorResponse.getHttpErrorCode()),
                () -> assertEquals(ErrorCode.INSUFFICIENT_BALANCE.getCode(),
                        errorResponse.getErrorCode()),
                () -> assertNotNull(errorResponse.getErrorMessage())
        );
    }

    @Ignore
    @Test
    void whenOldAccountHasBalanceBelow100_thenThrowValidErrorResponse() throws Exception {
        //TODO: Mock account data to mimic an old account. Did not implement due to time constraints.
        TransactionRequest request = new TransactionRequest()
                .accountNumber(randomAccount.getAccountNumber())
                .amount(50.0)
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
     * Helper method to create a random test account.
     *
     * @return random bank account
     */
    private AccountEntity createTestAccount(Double accountBalance) {

        return accountAPIService.createAccount(
                Double.doubleToLongBits(Math.random()),
                "Bob",
                "Builder",
                "GCE" + Math.random(),
                accountBalance
        );
    }

}