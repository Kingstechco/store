package com.securitease.store.exception;

/**
 * Exception thrown when a requested resource is not found in the system.
 *
 * <p>This runtime exception is typically used in service layers when attempting to retrieve entities by their
 * identifiers or other unique attributes. It provides structured error messages to help identify what resource was not
 * found and the criteria used in the search.
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new ResourceNotFoundException with a formatted message.
     *
     * <p>Creates a standardized error message in the format: "{resourceName} not found with {fieldName}: {fieldValue}"
     *
     * @param resourceName the type of resource that was not found (e.g., "Customer", "Order")
     * @param fieldName the field used to search for the resource (e.g., "id", "name")
     * @param fieldValue the value that was searched for but not found
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue));
    }
}
