package com.sisal.transaction.server.exception;

import com.sisal.transaction.server.util.ErrorCode;

public class AuthTimestampExpiredException extends AppServerBaseException {
    private final ErrorCode errorCode = ErrorCode.AUTH_TIMESTAMP_EXPIRED;

    public AuthTimestampExpiredException(String message) {
        super(message);
    }
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}