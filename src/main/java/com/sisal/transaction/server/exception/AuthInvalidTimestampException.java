package com.sisal.transaction.server.exception;

import com.sisal.transaction.server.util.ErrorCode;

public class AuthInvalidTimestampException extends AppServerBaseException {
    private final ErrorCode errorCode = ErrorCode.AUTH_TIMESTAMP_INVALID;

    public AuthInvalidTimestampException(String message) {
        super(message);
    }
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}