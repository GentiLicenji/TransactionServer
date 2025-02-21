package com.sisal.transaction.server.exception;

import com.sisal.transaction.server.util.ErrorCode;
import lombok.Getter;

@Getter
public class TransactionRateLimitException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.RATE_LIMIT_EXCEEDED;

    public TransactionRateLimitException(String message) {
        super(message);
    }

}