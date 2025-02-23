package com.sisal.transaction.server.util;

public enum ErrorCode {
    ACCOUNT_NOT_FOUND("ACC_001"),
    RATE_LIMIT_EXCEEDED("TXN_001"),
    INSUFFICIENT_BALANCE("TXN_002"),
    INVALID_TRANSACTION("TXN_003"),
    ANNOTATED_SCHEMA_FAILURE("VAL_001"),
    INVALID_REQUEST("VAL_002"),
    INVALID_REQUEST_HEADERS("VAL_003"),
    MISSING_REQUIRED_PARAMETER("VAL_004"),
    UNSUPPORTED_MEDIA_TYPE("VAL_005"),
    MALFORMED_JSON("VAL_006"),
    UNKOWN_ERROR("XXX_999");


    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
