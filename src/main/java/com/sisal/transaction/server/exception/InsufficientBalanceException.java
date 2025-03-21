package com.sisal.transaction.server.exception;

import com.sisal.transaction.server.util.ErrorCode;
import lombok.Getter;

@Getter
public class InsufficientBalanceException extends AppServerBaseException {
    private final ErrorCode errorCode = ErrorCode.INSUFFICIENT_BALANCE;

    public InsufficientBalanceException(String message) {
        super(message);
    }
    public ErrorCode getErrorCode() {
        return errorCode;
    }

}
