package com.example.store.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a business rule is violated.
 * <p>
 * This exception is used for scenarios where the request is technically valid
 * but violates business logic rules. It includes an error code to help clients
 * categorize and handle different business rule violations appropriately.
 * </p>
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
@Getter
public class BusinessRuleViolationException extends RuntimeException {

    private final String errorCode;

    /**
     * Constructs a new BusinessRuleViolationException with an error code and message.
     *
     * @param errorCode a specific code identifying the type of business rule violation
     * @param message the detail message explaining the business rule violation
     */
    public BusinessRuleViolationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new BusinessRuleViolationException with an error code, message, and cause.
     *
     * @param errorCode a specific code identifying the type of business rule violation
     * @param message the detail message explaining the business rule violation
     * @param cause the underlying cause of the exception
     */
    public BusinessRuleViolationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}