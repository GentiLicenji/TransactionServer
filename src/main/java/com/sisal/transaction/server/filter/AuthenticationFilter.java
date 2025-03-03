package com.sisal.transaction.server.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisal.transaction.server.config.auth.HmacAuthenticationToken;
import com.sisal.transaction.server.exception.AuthException;
import com.sisal.transaction.server.exception.AuthInvalidTimestampException;
import com.sisal.transaction.server.exception.AuthMissingHeaderException;
import com.sisal.transaction.server.exception.AuthTimestampExpiredException;
import com.sisal.transaction.server.model.rest.ErrorResponse;
import com.sisal.transaction.server.util.AuthUtil;
import com.sisal.transaction.server.util.ErrorCode;
import com.sisal.transaction.server.util.filter.CustomRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

/**
 * Authentication filter implementation based on Hash-based Message Authentication Code.
 */
public class AuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    private static final String HMAC_HEADER = "X-HMAC-SIGNATURE";
    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String TIMESTAMP_HEADER = "X-TIMESTAMP";
    private static final long MAX_TIMESTAMP_DIFF = 30 * 60 * 1000; // 30 minutes in milliseconds

    private final AuthenticationManager authenticationManager;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthenticationFilter(AuthenticationManager authenticationManager, ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException {
        try {

            // Create a wrapper to allow reading the request body multiple times
            CustomRequestWrapper requestWrapper = new CustomRequestWrapper(request);

            //Validate headers
            String apiKey = requestWrapper.getHeader(API_KEY_HEADER);
            String providedHmac = requestWrapper.getHeader(HMAC_HEADER);
            String timestamp = requestWrapper.getHeader(TIMESTAMP_HEADER);
            if (!StringUtils.hasText(apiKey)) {
                throw new AuthMissingHeaderException(API_KEY_HEADER + " is missing or empty!");
            }
            if (!StringUtils.hasText(providedHmac)) {
                throw new AuthMissingHeaderException(HMAC_HEADER + " is missing or empty!");
            }

            AuthUtil.validateTimestamp(timestamp, MAX_TIMESTAMP_DIFF);

            //Create request details
            HmacAuthenticationToken.RequestDetails details = new HmacAuthenticationToken.RequestDetails(
                    requestWrapper.getMethod(),
                    requestWrapper.getRequestURI(),
                    requestWrapper.getQueryString(),
                    AuthUtil.extractRequestBody(requestWrapper),
                    timestamp
            );

            HmacAuthenticationToken token = new HmacAuthenticationToken(apiKey, providedHmac, details);

            Authentication result = authenticationManager.authenticate(token);

            SecurityContextHolder.getContext().setAuthentication(result);

            filterChain.doFilter(requestWrapper, response);

        } catch (AuthMissingHeaderException | AuthInvalidTimestampException | AuthTimestampExpiredException |
                 AuthException e) {
            logger.error(e.getMessage(), e);
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, e.getErrorCode(), e.getMessage());
        } catch (BadCredentialsException e) {
            logger.error(e.getMessage(), e);
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_BAD_CREDENTIALS, e.getMessage());
        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.UNKOWN_ERROR, "Internal Unexpected Authentication error.Check Server logs for more details");
        }

    }

    /**
     * Sends REST error response for Auth failure.
     */
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, ErrorCode errorCode, String message) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse errorResponse = new ErrorResponse()
                .httpErrorCode(status.toString())
                .errorCode(errorCode.getCode())
                .errorMessage(message);

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}