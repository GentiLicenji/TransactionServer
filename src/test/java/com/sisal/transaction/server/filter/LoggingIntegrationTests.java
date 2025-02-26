package com.sisal.transaction.server.filter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisal.transaction.server.model.db.AccountEntity;
import com.sisal.transaction.server.model.rest.TransactionRequest;
import com.sisal.transaction.server.service.AccountApiService;
import com.sisal.transaction.server.util.TestUtils;
import com.sisal.transaction.test.config.TestConfig;
import com.sisal.transaction.test.config.TestDisableSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test suite for LoggingFilter functionality.
 * <p>
 * Validates request/response logging behavior by executing Transaction API calls,
 * ensuring proper logging and log masking of payload information.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestConfig.class, TestDisableSecurityConfig.class})//Loads H2 datasource and disables the SecurityFilter chain.
@ActiveProfiles("test")//Loads application-test.properties
        //TODO: Fix no security chain loading.
class LoggingIntegrationTests {

    private static final String BASE_URL = "/api/transactions";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private AccountApiService accountAPIService;
    private AccountEntity account;

    @Autowired
    private ListAppender<ILoggingEvent> memoryAppender;

    @BeforeEach
    void setUp() {
        memoryAppender.list.clear();
        account = createTestAccount();
    }

    @Test
    void whenValidTransactionCallWithSensitiveData_thenMaskSensitiveFields() throws Exception {
        //Perform API call
        TransactionRequest request = new TransactionRequest()
                .accountNumber(account.getAccountNumber())
                .amount(200.0)
                .transactionType(TransactionRequest.TransactionTypeEnum.DEPOSIT);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Verify appended logs
        List<String> logs = TestUtils.getFormattedLogs(memoryAppender);
        assertThat(logs).anyMatch(log -> log.contains("accountNumber=GB29**************6819"));
        assertThat(logs).anyMatch(log -> log.contains("\"accountNumber\":\"GB29**************6819\""));
        assertThat(logs).anyMatch(log -> log.contains("\"amount\":\"200.0\""));
        assertThat(logs).anyMatch(log -> log.contains("\"status\":\"COMPLETED\""));
        assertThat(logs).noneMatch(log -> log.contains("GB29NWBK60161331926819"));
    }

    @Test
    void whenRestCallsDifferentFromApiURLs_thenNoResponseLogShouldBeRecorded() throws Exception {

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andReturn();

        List<String> logs = TestUtils.getFormattedLogs(memoryAppender);
        assertThat(logs).noneMatch(log -> log.contains("{\"status\":\"UP\"}"));
    }

    /**
     * Helper method to create a test account with minimum balance.
     *
     * @return random bank account
     */
    private AccountEntity createTestAccount() {

        return accountAPIService.createAccount(
                1453454321358L,
                "Bob",
                "Builder",
                "DC29NWBK60161331926819",
                100.0
        );
    }
}