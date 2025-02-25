package com.sisal.transaction.server.exception;

import com.sisal.transaction.server.util.ErrorCode;

public class AuthSignatureException extends AppServerBaseException {
    private final ErrorCode errorCode = ErrorCode.AUTH_SIGNATURE;

    public AuthSignatureException(String message) {
        super(message);
    }

    public AuthSignatureException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }


}