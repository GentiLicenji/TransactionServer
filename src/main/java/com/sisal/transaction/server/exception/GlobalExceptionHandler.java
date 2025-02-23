package com.sisal.transaction.server.exception;

import com.sisal.transaction.server.model.rest.ErrorResponse;
import com.sisal.transaction.server.util.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse()
                .httpErrorCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
                .errorCode(ex.getErrorCode().getCode())
                .errorMessage(ex.getMessage());

        logger.error("Invalid account information. {}", ex.getMessage());
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
                .httpErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.toString())
                .errorCode(ErrorCode.UNKOWN_ERROR.getCode())
                .errorMessage(ex.getMessage());
        logger.error("Unexpected error: ", ex);
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * NOTE: We wouldn't want to propagate the error message outward as it is a potential vulnerability.
     * <p>
     * It can expose the ways our system validates, and it can expose package naming.
     *
     * @param validationException jackson validation failure
     * @return error response
     */
    @ExceptionHandler(value = {ConstraintViolationException.class})
    protected ResponseEntity<Object> handleAnnotatedValidationExceptions(ConstraintViolationException validationException) {

        String detailedErrorMessage = prepareValidationErrorMessage(validationException.getConstraintViolations());
        String errorMessage = "ConstraintViolation Exception: " + "Schema validation errors.Check logs for more details.";

        ErrorResponse errorResponse = new ErrorResponse()
                .httpErrorCode(HttpStatus.BAD_REQUEST.toString())
                .errorCode(ErrorCode.ANNOTATED_SCHEMA_FAILURE.getCode())
                .errorMessage(errorMessage);

        logger.error("Validation error: " + detailedErrorMessage, validationException);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Method logs and returns error response for schema validation failures on rest payload.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException methodArgumentNotValidException, HttpHeaders headers, HttpStatus status, WebRequest request) {
        String errorMessage = "MethodArgumentNotValid Exception: " + "Schema validation failures on http rest payload.Check logs for more details.";

        ErrorResponse errorResponse = new ErrorResponse()
                .httpErrorCode(HttpStatus.BAD_REQUEST.toString())
                .errorCode(ErrorCode.INVALID_REQUEST.getCode())
                .errorMessage(errorMessage);

        logger.error("MethodArgumentNotValid Exception:", methodArgumentNotValidException);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }


    /**
     * Convenience method is used to join validation error messages in a single string.
     */
    private String prepareValidationErrorMessage(Set<ConstraintViolation<?>> violations) {
        StringBuilder validationMessage = new StringBuilder();
        if (!violations.isEmpty()) {
            for (ConstraintViolation<?> violation : violations) {
                validationMessage.append(violation.getMessage());
            }
            return validationMessage.toString();
        } else
            return "No validation error messages were provided.";
    }
}
