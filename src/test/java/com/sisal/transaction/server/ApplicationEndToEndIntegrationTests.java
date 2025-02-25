package com.sisal.transaction.server;

import com.sisal.transaction.server.model.rest.ErrorResponse;
import com.sisal.transaction.server.model.rest.TransactionRequest;
import com.sisal.transaction.server.model.rest.TransactionResponse;
import com.sisal.transaction.server.util.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * End-to-End Integration Tests for the Application's Transaction API endpoints.
 *
 * <p>This test suite performs comprehensive integration testing by loading the complete
 * application context and configuration properties.
 * It validates the entire request-response
 * cycle including security mechanisms, logging, request processing, and response handling.</p>
 *
 * <p>Key features tested include:</p>
 * <ul>
 *   <li>HMAC Authentication</li>
 *   <li>API Key validation</li>
 *   <li>Transaction creation and processing</li>
 *   <li>Response structure and status codes</li>
 * </ul>
 *
 * <p>The test environment uses:</p>
 * <ul>
 *   <li>Random server port to avoid conflicts</li>
 *   <li>TestRestTemplate for HTTP requests</li>
 *   <li>Real application properties loaded from application.properties</li>
 * </ul>
 *
 * @see SpringBootTest
 * @see TestRestTemplate
 * @see ApiKeyProperties
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationEndToEndIntegrationTests {

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

    @BeforeEach
    void setUp() {
        headers = new HttpHeaders();
        transactionsUri = "http://localhost:" + port + "/api/transactions";
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(API_KEY_HEADER, ADMIN_API_KEY_VALUE);
        headers.add(TIMESTAMP_HEADER, String.valueOf(System.currentTimeMillis()));
    }

    @PostConstruct
    void setUpXMLParsing() {
        restTemplate.getRestTemplate().getMessageConverters()
                .add(new StringHttpMessageConverter());
    }

    @Test
    void createTransaction_Success_HappyPath() throws JsonProcessingException {
        // Given
        TransactionRequest request = new TransactionRequest()
                .accountNumber("GB29NWBK60161331926819")
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
                    assertThat(transaction.getAccountNumber()).isEqualTo("GB29NWBK60161331926819");
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
    void handleServletRequestBindingException_whenMissingRequiredHeader() {
        // Given
        TransactionRequest request = new TransactionRequest()
                .accountNumber("GB29NWBK60161331926819")
                .amount(60.0)
                .transactionType(TransactionRequest.TransactionTypeEnum.DEPOSIT);

        HttpHeaders headersWithoutHmac = new HttpHeaders();
        headersWithoutHmac.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TransactionRequest> httpEntity = new HttpEntity<>(request, headersWithoutHmac);

        // Perform REST API call
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                baseUrl,
                httpEntity,
                ErrorResponse.class
        );

        // Assert Result
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .isNotNull()
                .satisfies(error -> {
                    assertThat(error.getHttpErrorCode()).isEqualTo("400");
                    assertThat(error.getErrorMessage()).contains("Required request header 'X-HMAC-Signature' is not present");
                });
    }

    @Test
    void handleMissingServletRequestParameter_whenParameterMissing() {
        // Given
        Map<String, Object> incompleteRequest = new HashMap<>();
        incompleteRequest.put("accountNumber", "2342342342342343");
        // Missing required 'transactionType' parameter

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(incompleteRequest, headers);

        // Perform REST API call
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                baseUrl,
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

    @Test
    void handleHttpMediaTypeNotSupported_whenWrongContentType() {
        // Given
        HttpHeaders plainText = new HttpHeaders();
        plainText.setContentType(MediaType.TEXT_PLAIN);

        String plainTextContent = "My plain text content";

        HttpEntity<String> httpEntity = new HttpEntity<>(plainTextContent, plainText);

        // Perform REST API call
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                baseUrl,
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
    void handleHttpMessageNotReadable_WhenInvalidJson() {
        // Given
        String invalidJson = "{\"accountNumber\": \"ACC123456\", \"amount\": \"invalid\"";

        HttpEntity<String> httpEntity = new HttpEntity<>(invalidJson, headers);

        // Perform REST API call
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                baseUrl,
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
}
