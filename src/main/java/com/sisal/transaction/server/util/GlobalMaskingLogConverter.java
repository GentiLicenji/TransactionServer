package com.sisal.transaction.server.util;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced log masking implementation for sensitive data protection (PII/PCI compliance).
 * <p>
 * Controls application-wide logging because it intersects logback's conversion process
 * to mask sensitive data before it's written to any logging outputs. This converter
 * implements pattern-based masking for various types of sensitive information in both
 * JSON payloads and HTTP headers.
 * <p>
 * Masked Data Types:
 * - Account numbers in JSON payloads
 * - Transaction IDs in JSON payloads
 * - API Keys in HTTP headers
 * - HMAC signatures in HTTP headers
 * - Account numbers in error messages
 * <p>
 * Masking Strategy:
 * - Preserves first {@value #VISIBLE_FRONT} and last {@value #VISIBLE_BACK} characters
 * - Replaces middle characters with {@value #MASK}
 * - Special handling for values shorter than {@value #MIN_LENGTH} characters
 * - Maintains JSON structure and field names while masking only sensitive values
 * <p>
 * Configuration in logback.xml:
 * <conversionRule conversionWord="masked"
 * converterClass="com.sisal.transaction.server.util.GlobalMaskingLogConverter"/>
 * <p>
 * Pattern Layout Usage:
 * <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %masked%n</pattern>
 * <p>
 * Example Masking:
 * Input:  {"accountNumber": "1234567890123456"}
 * Output: {"accountNumber": "1234********3456"}
 *
 * @see ch.qos.logback.classic.pattern.ClassicConverter
 * @see ch.qos.logback.classic.spi.ILoggingEvent
 */
public class GlobalMaskingLogConverter extends ClassicConverter {

    private static final String MASK = "*";
    private static final int VISIBLE_FRONT = 4;
    private static final int VISIBLE_BACK = 4;
    private static final int MIN_LENGTH = 3;

    // JSON field patterns (3 groups)
    private static final Pattern ACCOUNT_NUMBER_PATTERN =
            Pattern.compile("(?i)(\"accountNumber\"\\s*:\\s*\")([^\"]*)(\")");
    private static final Pattern TRANSACTION_ID_PATTERN =
            Pattern.compile("(?i)(\"transactionId\"\\s*:\\s*\")([^\"]*)(\")");

    // Header patterns (2 groups)
    private static final Pattern API_KEY_PATTERN =
            Pattern.compile("(?i)(X-API-Key:?\\s*)([^,\\n]*)");
    private static final Pattern HMAC_PATTERN =
            Pattern.compile("(?i)(X-HMAC-Signature:?\\s*)([^,\\n]*)");

    // Error message pattern (2 groups)
    private static final Pattern ERROR_ACCOUNT_PATTERN =
            Pattern.compile("(accountNumber=)([A-Z0-9]{10,34})");

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (message == null) {
            return null;
        }

        message = maskPattern(message, ACCOUNT_NUMBER_PATTERN);
        message = maskPattern(message, TRANSACTION_ID_PATTERN);
        message = maskPattern(message, API_KEY_PATTERN);
        message = maskPattern(message, HMAC_PATTERN);
        message = maskPattern(message, ERROR_ACCOUNT_PATTERN);

        return message;
    }

    private String maskPattern(String message, Pattern pattern) {
        Matcher matcher = pattern.matcher(message);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String sensitiveData = matcher.group(2);
            String maskedValue = maskValue(sensitiveData);

            String replacement = matcher.groupCount() == 3
                    ? matcher.group(1) + maskedValue + matcher.group(3)
                    : matcher.group(1) + maskedValue;

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String maskValue(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        int length = value.length();

        // Handle short values
        if (length < MIN_LENGTH) {
            return repeatMask(length); // Mask everything if too short
        }

        // Handle values shorter than desired visible portions
        if (length <= VISIBLE_FRONT + VISIBLE_BACK) {
            int showChars = length / 2;
            return value.substring(0, showChars) +
                    repeatMask(length - showChars);
        }

        // Normal case: enough length for full masking pattern
        return value.substring(0, VISIBLE_FRONT) +
                repeatMask(length - VISIBLE_FRONT - VISIBLE_BACK) +
                value.substring(length - VISIBLE_BACK);
    }


    private String repeatMask(int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(MASK);
        }
        return sb.toString();
    }
}
