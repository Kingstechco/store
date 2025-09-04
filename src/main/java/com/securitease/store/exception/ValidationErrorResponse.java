package com.securitease.store.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Specialized error response for validation failures.
 *
 * <p>This class extends the standard error response format to include detailed validation error information. It
 * provides field-specific error messages to help clients understand exactly which validation constraints failed and how
 * to correct their input.
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 * @see ErrorResponse
 * @see GlobalExceptionHandler
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {
    private int status;
    private String message;
    private LocalDateTime timestamp;
    private Map<String, String> errors;
}
