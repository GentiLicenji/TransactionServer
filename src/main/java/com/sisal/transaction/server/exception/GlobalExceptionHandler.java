package com.sisal.transaction.server.exception;

import com.sisal.transaction.server.util.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse()
                .httpErrorCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
                .errorCode(ex.getErrorCode().getCode())
                .errorMessage(ex.getMessage());

        logger.error("Account not found: {}", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TransactionRateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(TransactionRateLimitException ex) {

        ErrorResponse errorResponse = new ErrorResponse()
                .httpErrorCode(String.valueOf(HttpStatus.TOO_MANY_REQUESTS.value()))
                .errorCode(ex.getErrorCode().getCode())
                .errorMessage(ex.getMessage());

        logger.error("Rate limit exceeded: {}", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(
            InsufficientBalanceException ex) {

        ErrorResponse errorResponse = new ErrorResponse()
                .httpErrorCode(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                .errorCode(ex.getErrorCode().getCode())
                .errorMessage(ex.getMessage());

        logger.error("Insufficient balance: {}", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {

        ErrorResponse errorResponse = new ErrorResponse()
                .httpErrorCode(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .errorCode(ErrorCode.INVALID_TRANSACTION.getCode())
                .errorMessage(ex.getMessage());
        logger.error("Unexpected error: ", ex);
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
