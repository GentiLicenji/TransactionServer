package com.sisal.transaction.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisal.transaction.server.config.ApiKeyProperties;
import com.sisal.transaction.server.model.db.AccountEntity;
import com.sisal.transaction.server.model.rest.ErrorResponse;
import com.sisal.transaction.server.model.rest.TransactionRequest;
import com.sisal.transaction.server.model.rest.TransactionResponse;
import com.sisal.transaction.server.service.AccountApiService;
import com.sisal.transaction.server.util.AuthUtil;
import com.sisal.transaction.server.util.ErrorCode;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * End-to-End Integration Tests for the Application's Transaction API endpoints.
 *
 * <p>This test suite performs comprehensive integration testing by loading the complete
 * application context and main configuration properties.
 *
 * <p>Tests complete request-response cycle including:</p>
 * <ul>
 *   <li>HMAC Authentication</li>
 *   <li>Request validation</li>
 *   <li>Transaction processing</li>
 *   <li>Error handling</li>
 * </ul>
 *
 * <p>Test Scenarios:</p>
 * <ul>
 *   <li>Successful transaction creation</li>
 *   <li>Invalid request parameters</li>
 *   <li>Invalid content types</li>
 *   <li>Malformed JSON handling</li>
 * </ul>
 *
 * <p>Uses random port and TestRestTemplate for HTTP requests.</p>
 *
 * @see SpringBootTest
 * @see TestRestTemplate
 * @see ApiKeyProperties
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TransactionAPIE2ETest {

    private static final String HMAC_HEADER = "X-HMAC-SIGNATURE";
    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String TIMESTAMP_HEADER = "X-TIMESTAMP";
    private static final String PATH = "/api/transactions";

    //Loading key from the application.properties file.
    @Value("${api.security.clients[0].api-key}")
    private String ADMIN_API_KEY_VALUE;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ApiKeyProperties apiKeyProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private HttpHeaders headers;
    private String transactionsUri;

    @Autowired
    private AccountApiService accountAPIService;
    private AccountEntity randomAccount;

    @BeforeEach
    void setUp() {
        headers = new HttpHeaders();
        transactionsUri = "http://localhost:" + port + "/api/transactions";
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(API_KEY_HEADER, ADMIN_API_KEY_VALUE);
        headers.add(TIMESTAMP_HEADER, String.valueOf(System.currentTimeMillis()));

        randomAccount = createTestAccount(100.0);
    }

    @Test
    public void createTransaction_Success_HappyPath() throws JsonProcessingException {
        // Given
        TransactionRequest request = new TransactionRequest()
                .accountNumber(randomAccount.getAccountNumber())
                .amount(60.0)
                .transactionType(TransactionRequest.TransactionTypeEnum.DEPOSIT);

        //Calculate auth signature
        String HmacSignature = AuthUtil.calculateHmac(HttpMethod.POST.name(),
                PATH,
                "",
                objectMapper.writeValueAsString(request),
                headers.getFirst(TIMESTAMP_HEADER),
                headers.getFirst(API_KEY_HEADER),
                apiKeyProperties.getClientByApiKey(ADMIN_API_KEY_VALUE).getSecretKey());
        headers.add(HMAC_HEADER, HmacSignature);

        HttpEntity<TransactionRequest> httpEntity = new HttpEntity<>(request, headers);

        // Perform REST API call
        ResponseEntity<TransactionResponse> response = restTemplate.postForEntity(
                transactionsUri,
                httpEntity,
                TransactionResponse.class
        );

        // Assert Result
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody())
                .isNotNull()
                .satisfies(transaction -> {
                    assertThat(transaction.getTransactionId()).isNotNull();
                    assertThat(transaction.getAccountNumber()).isEqualTo(randomAccount.getAccountNumber());
                    assertThat(transaction.getTransactionType()).isEqualTo(TransactionResponse.TransactionTypeEnum.DEPOSIT);
                    assertThat(transaction.getStatus()).isEqualTo(TransactionResponse.StatusEnum.COMPLETED);
                });
    }

    /**
     * ******************
     * Negative Scenarios
     * ******************
     */

    @Test
    @Ignore
    public void handleServletRequestBindingException_whenMissingRequiredHeader() {
//        TODO: requires more strict headers to test scenario. Change in spec required.
    }

    @Test
    public void handleMissingServletRequestParameter_whenParameterMissing() throws JsonProcessingException {
        // Missing required 'transactionType' parameter
        Map<String, Object> incompleteRequest = new HashMap<>();
        incompleteRequest.put("accountNumber", "2342342342342343");

        //Calculate auth signature
        String HmacSignature = AuthUtil.calculateHmac(HttpMethod.POST.name(),
                PATH,
                "",
                objectMapper.writeValueAsString(incompleteRequest),
                headers.getFirst(TIMESTAMP_HEADER),
                headers.getFirst(API_KEY_HEADER),
                apiKeyProperties.getClientByApiKey(ADMIN_API_KEY_VALUE).getSecretKey());
        headers.add(HMAC_HEADER, HmacSignature);


        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(incompleteRequest, headers);

        // Perform REST API call
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                transactionsUri,
                httpEntity,
                ErrorResponse.class
        );

        // Assert Result
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .isNotNull()
                .satisfies(error -> {
                    assertThat(error.getHttpErrorCode()).isEqualTo(HttpStatus.BAD_REQUEST.toString());
                    assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST.getCode());
                    assertThat(error.getErrorMessage()).contains("MethodArgumentNotValid Exception: Schema validation failures on http rest payload.Check logs for more details.");
                });
    }

    @Ignore
    @Test
    public void handleServletRequestBindingException_whenInvalidHeaders() {
        //TODO: we need to remove required mandatory business headers. Change in spec required.
        //We can't test this for now till new strict requirements come in.

        //Calculate auth signature
        String HmacSignature = AuthUtil.calculateHmac(HttpMethod.POST.name(),
                PATH,
                "",
                "",
                headers.getFirst(TIMESTAMP_HEADER),
                headers.getFirst(API_KEY_HEADER),
                apiKeyProperties.getClientByApiKey(ADMIN_API_KEY_VALUE).getSecretKey());
        headers.add(HMAC_HEADER, HmacSignature);

        HttpEntity<String> httpEntity = new HttpEntity<>("", headers);

        // Perform REST API call
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                transactionsUri,
                httpEntity,
                ErrorResponse.class
        );

        // Assert Result
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody())
                .isNotNull()
                .satisfies(error -> {
                    assertThat(error.getHttpErrorCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE.toString());
                    assertThat(error.getErrorCode()).isEqualTo(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode());
                    assertThat(error.getErrorMessage()).contains("HttpMediaTypeNotSupported Exception:");
                });
    }

    @Test
    public void handleHttpMediaTypeNotSupported_whenWrongContentType() {

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Create form data
        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("accountNumber", "GB29NWBK60161331926819");
        formData.add("amount", "60.0");
        formData.add("transactionType", "DEPOSIT");

        //Calculate auth signature
        String HmacSignature = AuthUtil.calculateHmac(HttpMethod.POST.name(),
                PATH,
                "",
                formData.toString(),
                headers.getFirst(TIMESTAMP_HEADER),
                headers.getFirst(API_KEY_HEADER),
                apiKeyProperties.getClientByApiKey(ADMIN_API_KEY_VALUE).getSecretKey());
        headers.add(HMAC_HEADER, HmacSignature);

        HttpEntity<String> httpEntity = new HttpEntity<>(formData.toString(), headers);

        // Perform REST API call
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                transactionsUri,
                httpEntity,
                ErrorResponse.class
        );

        // Assert Result
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody())
                .isNotNull()
                .satisfies(error -> {
                    assertThat(error.getHttpErrorCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE.toString());
                    assertThat(error.getErrorCode()).isEqualTo(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode());
                    assertThat(error.getErrorMessage()).contains("HttpMediaTypeNotSupported Exception:");
                });
    }

    @Test
    public void handleHttpMessageNotReadable_WhenInvalidJson() {
        // Given
        String invalidJson = "{\"accountNumber\": \"ACC123456\", \"amount\": \"invalid\"";

        //Calculate auth signature
        String HmacSignature = AuthUtil.calculateHmac(HttpMethod.POST.name(),
                PATH,
                "",
                invalidJson,
                headers.getFirst(TIMESTAMP_HEADER),
                headers.getFirst(API_KEY_HEADER),
                apiKeyProperties.getClientByApiKey(ADMIN_API_KEY_VALUE).getSecretKey());
        headers.add(HMAC_HEADER, HmacSignature);

        HttpEntity<String> httpEntity = new HttpEntity<>(invalidJson, headers);

        // Perform REST API call
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                transactionsUri,
                httpEntity,
                ErrorResponse.class
        );

        // Assert Result
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .isNotNull()
                .satisfies(error -> {
                    assertThat(error.getHttpErrorCode()).isEqualTo(HttpStatus.BAD_REQUEST.toString());
                    assertThat(error.getErrorCode()).isEqualTo(ErrorCode.MALFORMED_JSON.getCode());
                    assertThat(error.getErrorMessage()).contains("HttpMessageNotReadable Exception:");
                });
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
