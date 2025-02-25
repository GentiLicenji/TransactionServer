package com.sisal.transaction.server.exception;

import com.sisal.transaction.server.util.ErrorCode;


public class AuthException extends AppServerBaseException {
    private final ErrorCode errorCode = ErrorCode.AUTH_GENERIC;

    public AuthException(String message) {
        super(message);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

}