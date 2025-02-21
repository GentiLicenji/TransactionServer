package com.sisal.transaction.server.exception;

import com.sisal.transaction.server.util.ErrorCode;

public class AccountNotFoundException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.ACCOUNT_NOT_FOUND;

    public AccountNotFoundException(String message) {
        super(message);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

}
