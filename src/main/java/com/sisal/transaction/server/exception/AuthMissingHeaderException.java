package com.sisal.transaction.server.exception;

import com.sisal.transaction.server.util.ErrorCode;

public class AuthMissingHeaderException extends AppServerBaseException {
    private final ErrorCode errorCode = ErrorCode.AUTH_MISSING_HEADER;

    public AuthMissingHeaderException(String message) {
        super(message);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}