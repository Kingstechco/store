package com.securitease.store.exception;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Store application.
 *
 * <p>This class provides centralized exception handling across the entire application using Spring's
 * {@code @RestControllerAdvice}. It handles specific exceptions and converts them into appropriate HTTP responses with
 * consistent error formats.
 *
 * <p>Supported exception types:
 *
 * <ul>
 *   <li>{@link ResourceNotFoundException} - Returns 404 Not Found
 *   <li>{@link MethodArgumentNotValidException} - Returns 400 Bad Request with validation details
 *   <li>Generic {@link Exception} - Returns 500 Internal Server Error
 * </ul>
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 * @see ErrorResponse
 * @see ValidationErrorResponse
 */
@RestControllerAdvice(basePackages = "com.securitease.store.controller")
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
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
     * Handles validation exceptions and returns a 400 Bad Request response.
     *
     * @param ex the MethodArgumentNotValidException that was thrown
     * @param request the web request that triggered the exception
     * @return ResponseEntity containing ValidationErrorResponse with 400 status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {

        log.error("Validation exception: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Request validation failed",
                LocalDateTime.now(),
                errors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles constraint violation exceptions and returns a 400 Bad Request response.
     *
     * @param ex the ConstraintViolationException that was thrown
     * @param request the web request that triggered the exception
     * @return ResponseEntity containing ValidationErrorResponse with 400 status
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        log.error("Constraint violation exception: {}", ex.getMessage());

        Map<String, String> violations = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));

        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Request constraint validation failed",
                LocalDateTime.now(),
                violations);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles malformed JSON requests and returns a 400 Bad Request response.
     *
     * @param ex the HttpMessageNotReadableException that was thrown
     * @param request the web request that triggered the exception
     * @return ResponseEntity containing ErrorResponse with 400 status
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {

        log.error("Malformed request body: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "MALFORMED_JSON",
                "Request body is missing or malformed JSON",
                LocalDateTime.now(),
                request.getDescription(false));

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
     * Handles data access exceptions that occur during database operations.
     *
     * @param ex the DataAccessException that was thrown
     * @param request the web request that triggered the exception
     * @return ResponseEntity containing ErrorResponse with 500 status
     */
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(org.springframework.dao.DataAccessException ex, WebRequest request) {
        
        log.error("Database error occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "DATABASE_ERROR",
                "A database error occurred",
                LocalDateTime.now(),
                request.getDescription(false));

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles illegal argument exceptions and returns a 400 Bad Request response.
     *
     * @param ex the IllegalArgumentException that was thrown
     * @param request the web request that triggered the exception
     * @return ResponseEntity containing ErrorResponse with 400 status
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        
        log.error("Invalid argument: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_ARGUMENT",
                ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false));

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles illegal state exceptions and returns a 422 Unprocessable Entity response.
     *
     * @param ex the IllegalStateException that was thrown
     * @param request the web request that triggered the exception
     * @return ResponseEntity containing ErrorResponse with 422 status
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        
        log.error("Invalid state: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "INVALID_STATE",
                ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false));

        return new ResponseEntity<>(errorResponse, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Handles any other exceptions and returns a 500 Internal Server Error response.
     *
     * @param ex the Exception that was thrown
     * @param request the web request that triggered the exception
     * @return ResponseEntity containing ErrorResponse with 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        
        log.error("Unexpected exception: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                LocalDateTime.now(),
                request.getDescription(false));

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
