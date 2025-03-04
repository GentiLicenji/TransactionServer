package com.sisal.transaction.server.util;

import com.sisal.transaction.server.config.auth.HmacAuthenticationToken;
import com.sisal.transaction.server.exception.AuthInvalidTimestampException;
import com.sisal.transaction.server.exception.AuthTimestampExpiredException;
import com.sisal.transaction.server.util.filter.CustomRequestWrapper;
import org.springframework.security.authentication.BadCredentialsException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.stream.Collectors;

public class AuthUtil {

    public static void validateTimestamp(String timestampStr, long MAX_TIMESTAMP_DIFF) throws AuthTimestampExpiredException {
        try {
            long timestamp = Long.parseLong(timestampStr);
            if (timestamp <= 0) {
                throw new AuthInvalidTimestampException("Timestamp cannot be 0 or negative.");
            }
            long currentTime = System.currentTimeMillis();
            long diff = Math.abs(currentTime - timestamp);

            if (diff > MAX_TIMESTAMP_DIFF) {
                throw new AuthTimestampExpiredException("Timestamp expired");
            } else if (timestamp > currentTime) {
                throw new AuthTimestampExpiredException("Timestamp cannot be in the future");
            }
        } catch (NumberFormatException e) {
            throw new AuthInvalidTimestampException("Invalid timestamp format");
        }
    }

    public static String calculateHmac(HmacAuthenticationToken.RequestDetails requestDetails, String secretKey) {

        String queryString = requestDetails.getQueryString() != null ? requestDetails.getQueryString() : "";

        return calculateHmac(requestDetails.getMethod(),
                requestDetails.getPath(),
                queryString,
                requestDetails.getBody(),
                requestDetails.getTimestamp(),
                secretKey);
    }

    public static String calculateHmac(String method, String path, String queryString, String body, String timestamp, String secretKey) {

        // Construct the string to be signed
        String dataToSign = method + ":" + path + ":" + queryString +
                ":" + timestamp + ":" + body;

        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(keySpec);
            byte[] hmacBytes = hmac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new BadCredentialsException("Authentication failed: HMAC calculation failed", e);
        }
    }

    public static String extractRequestBody(CustomRequestWrapper requestWrapper) throws IOException {
        try (InputStreamReader inputStreamReader = new InputStreamReader(requestWrapper.getInputStream(), StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            return bufferedReader.lines()
                    .collect(Collectors.joining());
        }
    }
}
