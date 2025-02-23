package com.sisal.transaction.server.exception;

import com.sisal.transaction.server.model.rest.ErrorResponse;
import com.sisal.transaction.server.util.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
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
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException notFoundException) {

        ErrorResponse errorResponse = new ErrorResponse()
                .httpErrorCode(HttpStatus.NOT_FOUND.toString())
                .errorCode(notFoundException.getErrorCode().getCode())
                .errorMessage(notFoundException.getMessage());

        logger.error("Invalid account information. {}", notFoundException.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TransactionRateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(TransactionRateLimitException rateLimitException) {

        ErrorResponse errorResponse = new ErrorResponse()
                .httpErrorCode(HttpStatus.TOO_MANY_REQUESTS.toString())
                .errorCode(rateLimitException.getErrorCode().getCode())
                .errorMessage(rateLimitException.getMessage());

        logger.error("Rate limit exceeded: {}", rateLimitException.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException balanceException) {

        ErrorResponse errorResponse = new ErrorResponse()
                .httpErrorCode(HttpStatus.CONFLICT.toString())
                .errorCode(balanceException.getErrorCode().getCode())
                .errorMessage(balanceException.getMessage());

        logger.error("Insufficient balance: {}", balanceException.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception exception) {

        ErrorResponse errorResponse = new ErrorResponse()
                .httpErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.toString())
                .errorCode(ErrorCode.UNKOWN_ERROR.getCode())
                .errorMessage(exception.getMessage());

        logger.error("Unexpected error: ", exception);

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
        String errorMessage = "ConstraintViolation Exception: Schema validation errors.Check logs for more details.";

        ErrorResponse errorResponse = new ErrorResponse()
                .httpErrorCode(HttpStatus.BAD_REQUEST.toString())
                .errorCode(ErrorCode.ANNOTATED_SCHEMA_FAILURE.getCode())
                .errorMessage(errorMessage);

        logger.error("Validation error: {}", detailedErrorMessage, validationException);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Method logs and returns error response for schema validation failures on rest payload.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException methodArgumentNotValidException, HttpHeaders headers, HttpStatus status, WebRequest request) {
        String errorMessage = "MethodArgumentNotValid Exception: Schema validation failures on http rest payload.Check logs for more details.";

        ErrorResponse errorResponse = new ErrorResponse()
                .httpErrorCode(HttpStatus.BAD_REQUEST.toString())
                .errorCode(ErrorCode.INVALID_REQUEST.getCode())
                .errorMessage(errorMessage);

        logger.error("MethodArgumentNotValid Exception: Schema validation failures on http rest payload.", methodArgumentNotValidException);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Method logs and returns error response when request is incomplete (header/binding issue)
     */
    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException servletRequestBindingException,
                                                                          HttpHeaders headers, HttpStatus status, WebRequest request) {
        String errorMessage = "ServletRequestBinding Exception: Http request has header/binding issues.Check logs for more details.";

        ErrorResponse errorResponse = new ErrorResponse()
                .httpErrorCode(status.toString())
                .errorCode(ErrorCode.INVALID_REQUEST_HEADERS.getCode())
                .errorMessage(errorMessage);

        logger.error("ServletRequestBinding Exception: Http request has header/binding issues.", servletRequestBindingException);

        return new ResponseEntity<>(errorResponse, headers, status);
    }

    /**
     * Method handles missing required request parameters.
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException missingServletRequestParameterException,
                                                                          HttpHeaders headers, HttpStatus status, WebRequest request) {

        String errorMessage = "MissingServletRequestParameter Exception: Missing required request params.Check logs for more details.";

        ErrorResponse errorResponse = new ErrorResponse()
                .httpErrorCode(status.toString())
                .errorCode(ErrorCode.MISSING_REQUIRED_PARAMETER.getCode())
                .errorMessage(errorMessage);

        logger.error("MissingServletRequestParameter Exception: Missing request parameter ({}) {} in request.", missingServletRequestParameterException.getParameterType(), missingServletRequestParameterException.getParameterName(), missingServletRequestParameterException);

        return new ResponseEntity<>(errorResponse, headers, status);
    }

    /**
     * Method handles the wrong media type for API.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException mediaTypeNotSupportedException,
                                                                     HttpHeaders headers, HttpStatus status, WebRequest request) {

        String errorMessage = "HttpMediaTypeNotSupported Exception: Content type (" + mediaTypeNotSupportedException.getContentType() + ") not supported on request.";
        ErrorResponse notSupportedErrorResponse = new ErrorResponse()
                .errorMessage(errorMessage)
                .httpErrorCode(status.toString())
                .errorCode(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode());

        logger.error("HttpMediaTypeNotSupported Exception: Content type ({}) not supported on request.", mediaTypeNotSupportedException.getContentType(), mediaTypeNotSupportedException);

        return new ResponseEntity<>(notSupportedErrorResponse, headers, status);
    }

    /**
     * Method logs and returns error response for reading failures on rest payload.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException messageNotReadableException,
                                                                  HttpHeaders headers, HttpStatus status, WebRequest request) {

        String errorMessage = "HttpMessageNotReadable Exception: Reading rest payload failed.Check logs for more details.";

        ErrorResponse errorResponse = new ErrorResponse()
                .errorMessage(errorMessage)
                .httpErrorCode(status.toString())
                .errorCode(ErrorCode.MALFORMED_JSON.getCode());

        logger.error("HttpMessageNotReadable Exception: Reading rest payload failed.", messageNotReadableException);

        return new ResponseEntity<>(errorResponse, headers, status);
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
