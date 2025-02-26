package com.sisal.transaction.server.filter;

import com.sisal.transaction.server.util.filter.CustomRequestWrapper;
import com.sisal.transaction.server.util.filter.CustomResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        CustomRequestWrapper requestWrapper = new CustomRequestWrapper(request);
        CustomResponseWrapper responseWrapper = new CustomResponseWrapper(response);

        // Generate a unique trace ID for the request
        // In an enterprise solution this should be coming in from the client side as an X-CorrelationId Header.
        String traceId = UUID.randomUUID().toString();

        logRequest(requestWrapper, traceId);

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {

            logResponse(responseWrapper, traceId);

        }
    }

    private void logRequest(CustomRequestWrapper requestWrapper, String traceId) throws IOException {

        logger.info(
                MessageFormat.format(
                        "Transaction Server Request Log: " +
                                "TraceID: {0} | " +
                                "Timestamp: {1} | " +
                                "Method: {2} | " +
                                "URI: {3} | " +
                                "API-Key: {4} | " +
                                "HMAC-Signature: {5} | " +
                                "Timestamp-Header: {6} | " +
                                "Content-Type: {7} | " +
                                "Request-Body: {8}",
                        traceId,
                        Instant.now().toString(),
                        requestWrapper.getMethod(),
                        requestWrapper.getRequestURI(),
                        requestWrapper.getHeader("X-API-Key"),
                        requestWrapper.getHeader("X-HMAC-Signature"),
                        requestWrapper.getHeader("X-Timestamp"),
                        requestWrapper.getContentType(),
                        extractRequestBody(requestWrapper))
        );
    }

    private void logResponse(CustomResponseWrapper responseWrapper, String traceId) {
        logger.info(
                MessageFormat.format(
                        "Transaction Server Response Log: " +
                                "TraceID: {0} | " +
                                "Timestamp: {1} | " +
                                "Status: {2} | " +
                                "Content-Type: {3} | " +
                                "Request-Body: {4}",
                        traceId,
                        Instant.now().toString(),
                        responseWrapper.getStatus(),
                        responseWrapper.getContentType(),
                        extractResponseBody(responseWrapper))
        );
    }

    private String extractRequestBody(CustomRequestWrapper requestWrapper) throws IOException {
        InputStreamReader inputStreamReader = null;
        try {
            inputStreamReader = new InputStreamReader(requestWrapper.getInputStream(), StandardCharsets.UTF_8);
            return new BufferedReader(inputStreamReader)
                    .lines()
                    .collect(Collectors.joining(" "));
        } finally {
            inputStreamReader.close();
        }

    }

    private String extractResponseBody(CustomResponseWrapper responseWrapper) {
        byte[] responseBodyByte = responseWrapper.getContentAsByteArray();

        // Get the response body
        String responseBody = "No body";

        if (responseBodyByte.length > 0) {
            responseBody = new String(responseBodyByte, StandardCharsets.UTF_8);
        }

        //Stripping new lines
        return StringUtils.trimWhitespace(
                responseBody.replace('\n', ' ').replace('\r', ' ')
        );
    }
}