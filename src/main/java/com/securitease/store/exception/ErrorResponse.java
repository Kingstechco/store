package com.example.store.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard error response structure for REST API endpoints.
 * <p>
 * This class provides a consistent format for error responses across the application.
 * It includes essential information about the error including HTTP status code,
 * error message, timestamp, and the request path that caused the error.
 * </p>
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 * @see GlobalExceptionHandler
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
    private String path;
}
