package com.sisal.transaction.server.util;

import com.sisal.transaction.server.exception.AuthSignatureException;
import com.sisal.transaction.server.exception.AuthTimestampExpiredException;
import com.sisal.transaction.server.util.logging.CustomRequestWrapper;

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
            long currentTime = System.currentTimeMillis();
            long diff = Math.abs(currentTime - timestamp);

            if (diff > MAX_TIMESTAMP_DIFF) {
                throw new AuthTimestampExpiredException("Timestamp expired");
            }
        } catch (NumberFormatException e) {
            throw new AuthTimestampExpiredException("Invalid timestamp format");
        }
    }

    public static String calculateHmac(CustomRequestWrapper request, String timestamp, String apiKeyHeaderName, String secretKey) throws IOException {
        // Create canonical request string including all elements to be verified
        String method = request.getMethod();
        String path = request.getRequestURI();
        String queryString = request.getQueryString() != null ? request.getQueryString() : "";
        String apiKey = request.getHeader(apiKeyHeaderName);

        // Get request body
        String body = extractRequestBody(request);

        return calculateHmac(method, path, queryString, body, timestamp, apiKey, secretKey);
    }

    public static String calculateHmac(String method, String path, String queryString, String body, String timestamp, String apiKey, String secretKey) {

        // Construct the string to be signed
        String dataToSign = method + ":" + path + ":" + queryString + ":" +
                apiKey + ":" + timestamp + ":" + body;

        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(keySpec);
            byte[] hmacBytes = hmac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new AuthSignatureException("HMAC calculation failed", e);
        }
    }

    private static String extractRequestBody(CustomRequestWrapper requestWrapper) throws IOException {
        InputStreamReader inputStreamReader = null;
        try {
            inputStreamReader = new InputStreamReader(requestWrapper.getInputStream(), StandardCharsets.UTF_8);
            return new BufferedReader(inputStreamReader)
                    .lines()
                    .collect(Collectors.joining(" "));
        } finally {
            assert inputStreamReader != null;
            inputStreamReader.close();
        }

    }
}
