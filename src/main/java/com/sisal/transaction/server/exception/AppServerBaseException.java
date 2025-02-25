package com.sisal.transaction.server.exception;

import com.sisal.transaction.server.util.ErrorCode;

public abstract class AppServerBaseException extends RuntimeException {

    public AppServerBaseException(String message) {
        super(message);
    }

    public AppServerBaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public AppServerBaseException(Throwable cause) {
        super(cause);
    }

    public AppServerBaseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public abstract ErrorCode getErrorCode();
}
