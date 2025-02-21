package com.sisal.transaction.server.util;

public enum ErrorCode {
    ACCOUNT_NOT_FOUND("ACC_001"),
    RATE_LIMIT_EXCEEDED("TXN_001"),
    INSUFFICIENT_BALANCE("TXN_002"),
    INVALID_TRANSACTION("TXN_003");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
