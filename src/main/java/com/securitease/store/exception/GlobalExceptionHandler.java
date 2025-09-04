package com.example.store.exception;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the Store application.
 * <p>
 * This class provides centralized exception handling across the entire application
 * using Spring's {@code @RestControllerAdvice}. It handles specific exceptions
 * and converts them into appropriate HTTP responses with consistent error formats.
 * </p>
 * <p>
 * Supported exception types:
 * <ul>
 *   <li>{@link ResourceNotFoundException} - Returns 404 Not Found</li>
 *   <li>{@link MethodArgumentNotValidException} - Returns 400 Bad Request with validation details</li>
 *   <li>Generic {@link Exception} - Returns 500 Internal Server Error</li>
 * </ul>
 * </p>
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 * @see ErrorResponse
 * @see ValidationErrorResponse
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles ResourceNotFoundException and returns a 404 Not Found response.
     *
     * @param ex the ResourceNotFoundException that was thrown
     * @param request the web request that triggered the exception
     * @return ResponseEntity containing ErrorResponse with 404 status
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {

        log.error("Resource not found: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(), 
                "RESOURCE_NOT_FOUND", 
                ex.getMessage(), 
                LocalDateTime.now(), 
                request.getDescription(false));

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles validation errors and returns a 400 Bad Request response.
     *
     * @param ex the MethodArgumentNotValidException containing validation errors
     * @return ResponseEntity containing ValidationErrorResponse with 400 status and field-specific error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {

        log.error("Validation failed: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(), "Validation failed", LocalDateTime.now(), errors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles CustomerAlreadyExistsException and returns a 409 Conflict response.
     *
     * @param ex the CustomerAlreadyExistsException that was thrown
     * @param request the web request that triggered the exception
     * @return ResponseEntity containing ErrorResponse with 409 status
     */
    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCustomerAlreadyExists(
            CustomerAlreadyExistsException ex, WebRequest request) {

        log.error("Customer already exists: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "CUSTOMER_ALREADY_EXISTS",
                ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false));

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handles BusinessRuleViolationException and returns a 422 Unprocessable Entity response.
     *
     * @param ex the BusinessRuleViolationException that was thrown
     * @param request the web request that triggered the exception
     * @return ResponseEntity containing ErrorResponse with 422 status
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolation(
            BusinessRuleViolationException ex, WebRequest request) {

        log.error("Business rule violation: {} - {}", ex.getErrorCode(), ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false));

        return new ResponseEntity<>(errorResponse, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Handles all unhandled exceptions and returns a 500 Internal Server Error response.
     * <p>
     * This is a catch-all handler for any exceptions not specifically handled by other methods.
     * It ensures that no exception goes unhandled and provides a consistent error response format.
     * </p>
     *
     * @param ex the unhandled exception
     * @param request the web request that triggered the exception
     * @return ResponseEntity containing ErrorResponse with 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {

        log.error("Unexpected error occurred", ex);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                LocalDateTime.now(),
                request.getDescription(false));

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
