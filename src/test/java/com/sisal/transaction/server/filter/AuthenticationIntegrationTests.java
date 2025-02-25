package com.sisal.transaction.server.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisal.transaction.server.model.api.TransactionAPIRequest;
import com.sisal.transaction.server.model.api.TransactionAPIResponse;
import com.sisal.transaction.server.model.rest.ErrorResponse;
import com.sisal.transaction.server.model.rest.TransactionRequest;
import com.sisal.transaction.server.service.TransactionApiService;
import com.sisal.transaction.server.util.AuthUtil;
import com.sisal.transaction.server.util.ErrorCode;
import com.sisal.transaction.test.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test Suite for Authentication and Authorization Mechanisms
 *
 * <p>This test suite performs comprehensive integration testing of the application's
 * authentication filter chain, focusing on API key validation, HMAC signature verification,
 * and timestamp validation. It uses a full application context with a mock MVC configuration
 * for testing HTTP request processing.</p>
 *
 * <p>Test Configuration:</p>
 * <ul>
 *   <li>Uses Spring Boot Test context with MockMVC</li>
 *   <li>Imports custom test configuration via {@link TestConfig}</li>
 *   <li>Uses a test-specific properties file through 'test' profile</li>
 * </ul>
 *
 * <p>Test Scenarios Covered:</p>
 * <ul>
 *   <li>Authentication Success:
 *     <ul>
 *       <li>Valid API key, HMAC signature, and timestamp</li>
 *       <li>Proper request processing through the filter chain</li>
 *     </ul>
 *   </li>
 *   <li>API Key Validation:
 *     <ul>
 *       <li>Missing API key header scenarios</li>
 *       <li>Invalid API key format or non-existent client</li>
 *     </ul>
 *   </li>
 *   <li>HMAC Authentication:
 *     <ul>
 *       <li>Missing HMAC signature header</li>
 *       <li>Invalid or malformed HMAC signatures</li>
 *       <li>Signature verification failures</li>
 *     </ul>
 *   </li>
 *   <li>Timestamp Validation:
 *     <ul>
 *       <li>Missing timestamp header scenarios</li>
 *       <li>Invalid timestamp format handling</li>
 *       <li>Expired or future-dated timestamp handling</li>
 *     </ul>
 *   </li>
 *   <li>Error Handling:
 *     <ul>
 *       <li>Filter chain execution failures</li>
 *       <li>Error response format verification</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>Each test case validates both the response status codes and error messages
 * to ensure proper error handling and client feedback.</p>
 *
 * @author Gentian Licenji
 * @version 1.0
 * @see TestConfig
 * @see MockMvc
 * @see SpringBootTest
 * @see AutoConfigureMockMvc
 * @since 2025-02-25
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
@ActiveProfiles("test")
public class AuthenticationIntegrationTests {

    private static final String HMAC_HEADER = "X-HMAC-SIGNATURE";
    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String TIMESTAMP_HEADER = "X-TIMESTAMP";
    private static final String PATH = "/api/transactions";

    @Value("${api.security.clients[0].api-key}")
    private String ADMIN_API_KEY_VALUE;

    @Value("${api.security.clients[0].secret-key}")
    private String ADMIN_SECRET_VALUE;

    private String TIMESTAMP_VALUE;
    private String HMAC_SIGNATURE_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionApiService transactionApiService;

    @BeforeEach
    void setUp() {

        TIMESTAMP_VALUE = String.valueOf(System.currentTimeMillis());

        //Calculate auth signature
        HMAC_SIGNATURE_VALUE = AuthUtil.calculateHmac(HttpMethod.POST.name(),
                PATH,
                "",
                "",
                TIMESTAMP_VALUE,
                ADMIN_API_KEY_VALUE,
                ADMIN_SECRET_VALUE);
    }

    @Test
    public void whenValidAuthHeader_thenDontReturnErrorResponse() throws Exception {
        // Mock the service call
        when(transactionApiService.createTransaction(any(TransactionAPIRequest.class)))
                .thenReturn(any(TransactionAPIResponse.class));

        TransactionRequest request = new TransactionRequest()
                .accountNumber("MockAccount")
                .transactionType(TransactionRequest.TransactionTypeEnum.DEPOSIT)
                .amount(1000.0);
        String jsonRequest=objectMapper.writeValueAsString(request);

        //Re-Calculate auth signature
        HMAC_SIGNATURE_VALUE = AuthUtil.calculateHmac(HttpMethod.POST.name(),
                PATH,
                "",
                jsonRequest,
                TIMESTAMP_VALUE,
                ADMIN_API_KEY_VALUE,
                ADMIN_SECRET_VALUE);

            MvcResult result = mockMvc.perform(post(PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(API_KEY_HEADER, ADMIN_API_KEY_VALUE)
                            .header(HMAC_HEADER, HMAC_SIGNATURE_VALUE)
                            .header(TIMESTAMP_HEADER, TIMESTAMP_VALUE)
                            .content(jsonRequest))
                    .andExpect(status().isCreated())
                    .andReturn();

            assertFalse(result.getResponse().getContentAsString().contains("errorCode"),
                    "Response should not contain ErrorResponse fields");
    }

    @Test
    public void whenMissingApiKeyHeader_thenThrowValidErrorResponse() throws Exception {
        
        MvcResult result = mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HMAC_HEADER, HMAC_SIGNATURE_VALUE)
                        .header(TIMESTAMP_HEADER, TIMESTAMP_VALUE)
                        .content(""))
                .andExpect(status().isUnauthorized())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertAll(
                () -> assertEquals(HttpStatus.UNAUTHORIZED.toString(), errorResponse.getHttpErrorCode()),
                () -> assertEquals(ErrorCode.AUTH_MISSING_HEADER.getCode(),
                        errorResponse.getErrorCode()),
                () -> assertEquals(API_KEY_HEADER + " is missing or empty!", errorResponse.getErrorMessage())
        );
    }

    @Test
    public void whenInvalidApiKeyHeader_thenThrowValidErrorResponse() throws Exception {

        MvcResult result = mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(API_KEY_HEADER, "WrongApiKey")
                        .header(HMAC_HEADER, HMAC_SIGNATURE_VALUE)
                        .header(TIMESTAMP_HEADER, TIMESTAMP_VALUE)
                        .content(""))
                .andExpect(status().isUnauthorized())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertAll(
                () -> assertEquals(HttpStatus.UNAUTHORIZED.toString(), errorResponse.getHttpErrorCode()),
                () -> assertEquals(ErrorCode.AUTH_GENERIC.getCode(),
                        errorResponse.getErrorCode()),
                () -> assertEquals("Invalid API key", errorResponse.getErrorMessage())
        );
    }

    @Test
    public void whenMissingHmacHeader_thenThrowValidErrorResponse() throws Exception {

//        doNothing()
//                .when(filterChain)
//                .doFilter(any(CustomRequestWrapper.class), any(HttpServletResponse.class));

        MvcResult result = mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(API_KEY_HEADER, ADMIN_API_KEY_VALUE)
                        .header(TIMESTAMP_HEADER, TIMESTAMP_VALUE)
                        .content(""))
                .andExpect(status().isUnauthorized())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertAll(
                () -> assertEquals(HttpStatus.UNAUTHORIZED.toString(), errorResponse.getHttpErrorCode()),
                () -> assertEquals(ErrorCode.AUTH_MISSING_HEADER.getCode(),
                        errorResponse.getErrorCode()),
                () -> assertEquals(HMAC_HEADER + " is missing or empty!", errorResponse.getErrorMessage())
        );
    }

    @Test
    public void whenInvalidHmacHeader_thenThrowValidErrorResponse() throws Exception {

        MvcResult result = mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(API_KEY_HEADER, ADMIN_API_KEY_VALUE)
                        .header(HMAC_HEADER, "XXX823897324798234987&*%$3")
                        .header(TIMESTAMP_HEADER, TIMESTAMP_VALUE)
                        .content(""))
                .andExpect(status().isUnauthorized())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertAll(
                () -> assertEquals(HttpStatus.UNAUTHORIZED.toString(), errorResponse.getHttpErrorCode()),
                () -> assertEquals(ErrorCode.AUTH_SIGNATURE.getCode(),
                        errorResponse.getErrorCode()),
                () -> assertEquals("Invalid HMAC signature", errorResponse.getErrorMessage())
        );
    }

    @Test
    public void whenMissingTimestampHeader_thenThrowValidErrorResponse() throws Exception {

        MvcResult result = mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(API_KEY_HEADER, ADMIN_API_KEY_VALUE)
                        .header(HMAC_HEADER, HMAC_SIGNATURE_VALUE)
                        .content(""))
                .andExpect(status().isUnauthorized())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertAll(
                () -> assertEquals(HttpStatus.UNAUTHORIZED.toString(), errorResponse.getHttpErrorCode()),
                () -> assertEquals(ErrorCode.AUTH_TIMESTAMP_INVALID.getCode(),
                        errorResponse.getErrorCode()),
                () -> assertEquals("Invalid timestamp format", errorResponse.getErrorMessage())
        );
    }

    @Test
    public void whenInvalidTimestampHeader_thenThrowValidErrorResponse() throws Exception {

        String invalidTimeStamp="sX%2df34234";

        //Re-Calculate auth signature
        HMAC_SIGNATURE_VALUE = AuthUtil.calculateHmac(HttpMethod.POST.name(),
                PATH,
                "",
                "",
                invalidTimeStamp,
                ADMIN_API_KEY_VALUE,
                ADMIN_SECRET_VALUE);

        MvcResult result = mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(API_KEY_HEADER, ADMIN_API_KEY_VALUE)
                        .header(HMAC_HEADER, HMAC_SIGNATURE_VALUE)
                        .header(TIMESTAMP_HEADER, invalidTimeStamp)
                        .content(""))
                .andExpect(status().isUnauthorized())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertAll(
                () -> assertEquals(HttpStatus.UNAUTHORIZED.toString(), errorResponse.getHttpErrorCode()),
                () -> assertEquals(ErrorCode.AUTH_TIMESTAMP_INVALID.getCode(),
                        errorResponse.getErrorCode()),
                () -> assertEquals("Invalid timestamp format", errorResponse.getErrorMessage())
        );
    }

    @Test
    public void whenNegativeZeroTimestampHeader_thenThrowValidErrorResponse() throws Exception {

        String invalidTimeStamp="-1";

        //Re-Calculate auth signature
        HMAC_SIGNATURE_VALUE = AuthUtil.calculateHmac(HttpMethod.POST.name(),
                PATH,
                "",
                "",
                invalidTimeStamp,
                ADMIN_API_KEY_VALUE,
                ADMIN_SECRET_VALUE);

        MvcResult result = mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(API_KEY_HEADER, ADMIN_API_KEY_VALUE)
                        .header(HMAC_HEADER, HMAC_SIGNATURE_VALUE)
                        .header(TIMESTAMP_HEADER, invalidTimeStamp)
                        .content(""))
                .andExpect(status().isUnauthorized())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertAll(
                () -> assertEquals(HttpStatus.UNAUTHORIZED.toString(), errorResponse.getHttpErrorCode()),
                () -> assertEquals(ErrorCode.AUTH_TIMESTAMP_INVALID.getCode(),
                        errorResponse.getErrorCode()),
                () -> assertEquals("Timestamp cannot be 0 or negative.", errorResponse.getErrorMessage())
        );
    }

    @Test
    public void whenOldTimestampHeader_thenThrowValidErrorResponse() throws Exception {

        // 31 minutes in the past
        String pastTimestamp = String.valueOf(System.currentTimeMillis() - (31 * 60 * 1000));

        //Re-Calculate auth signature
        HMAC_SIGNATURE_VALUE = AuthUtil.calculateHmac(HttpMethod.POST.name(),
                PATH,
                "",
                "",
                pastTimestamp,
                ADMIN_API_KEY_VALUE,
                ADMIN_SECRET_VALUE);

        MvcResult result = mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(API_KEY_HEADER, ADMIN_API_KEY_VALUE)
                        .header(HMAC_HEADER, HMAC_SIGNATURE_VALUE)
                        .header(TIMESTAMP_HEADER, pastTimestamp)
                        .content(""))
                .andExpect(status().isUnauthorized())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertAll(
                () -> assertEquals(HttpStatus.UNAUTHORIZED.toString(), errorResponse.getHttpErrorCode()),
                () -> assertEquals(ErrorCode.AUTH_TIMESTAMP_EXPIRED.getCode(),
                        errorResponse.getErrorCode()),
                () -> assertEquals("Timestamp expired", errorResponse.getErrorMessage())
        );
    }

    @Test
    public void whenFutureTimestampHeader_thenThrowValidErrorResponse() throws Exception {

        // 30 minutes in the future
        String futureTimestamp = String.valueOf(System.currentTimeMillis() + (30 * 60 * 1000));

        //Re-Calculate auth signature
        HMAC_SIGNATURE_VALUE = AuthUtil.calculateHmac(HttpMethod.POST.name(),
                PATH,
                "",
                "",
                futureTimestamp,
                ADMIN_API_KEY_VALUE,
                ADMIN_SECRET_VALUE);

        MvcResult result = mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(API_KEY_HEADER, ADMIN_API_KEY_VALUE)
                        .header(HMAC_HEADER, HMAC_SIGNATURE_VALUE)
                        .header(TIMESTAMP_HEADER, futureTimestamp)
                        .content(""))
                .andExpect(status().isUnauthorized())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertAll(
                () -> assertEquals(HttpStatus.UNAUTHORIZED.toString(), errorResponse.getHttpErrorCode()),
                () -> assertEquals(ErrorCode.AUTH_TIMESTAMP_EXPIRED.getCode(),
                        errorResponse.getErrorCode()),
                () -> assertEquals("Timestamp cannot be in the future", errorResponse.getErrorMessage())
        );
    }

    @Test
    public void whenFilterChainFails_thenThrowValidErrorResponse() throws Exception {

//        doNothing()
//                .when(filterChain)
//                .doFilter(any(CustomRequestWrapper.class), any(HttpServletResponse.class));

        MvcResult result = mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
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

}
