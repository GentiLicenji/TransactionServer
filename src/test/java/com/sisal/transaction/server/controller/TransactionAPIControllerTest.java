package com.sisal.transaction.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisal.transaction.server.model.rest.TransactionRequest;
import com.sisal.transaction.server.util.TestConfig;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:transaction.spring.properties")
@Import(TestConfig.class)
public class TransactionAPIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/transactions";
    private static final String VALID_API_KEY = "test-api-key";
    private static final String VALID_HMAC = "valid-hmac-signature";

    @Test
    public void whenCreateValidTransaction_thenReturnSuccess() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountId("ACC123");
        request.setTransactionType(TransactionRequest.TransactionTypeEnum.DEPOSIT);
        request.setAmount(1000.0);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-Key", VALID_API_KEY)
                        .header("X-HMAC-Signature", VALID_HMAC)
                        .header("X-Timestamp", String.valueOf(System.currentTimeMillis()))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.accountId").value("ACC123"))
                .andExpect(jsonPath("$.amount").value(1000.0));
    }

    @Test
    public void whenCreateTransactionWithInvalidAmount_thenReturnBadRequest() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountId("ACC123");
        request.setTransactionType(TransactionRequest.TransactionTypeEnum.DEPOSIT);
        request.setAmount(11000.0); // Exceeds maximum

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-Key", VALID_API_KEY)
                        .header("X-HMAC-Signature", VALID_HMAC)
                        .header("X-Timestamp", String.valueOf(System.currentTimeMillis()))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").exists())
                .andExpect(jsonPath("$.errorMessage").exists());
    }

    @Test
    public void whenMissingAuthHeaders_thenReturnUnauthorized() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountId("ACC123");
        request.setTransactionType(TransactionRequest.TransactionTypeEnum.DEPOSIT);
        request.setAmount(1000.0);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}