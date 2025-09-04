package com.example.store.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting to create a customer that already exists.
 * <p>
 * This exception is used in business logic scenarios where duplicate customers
 * are not allowed based on business rules such as unique names or email addresses.
 * </p>
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class CustomerAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new CustomerAlreadyExistsException with a formatted message.
     *
     * @param name the name of the customer that already exists
     */
    public CustomerAlreadyExistsException(String name) {
        super("Customer with name '" + name + "' already exists");
    }

    /**
     * Constructs a new CustomerAlreadyExistsException with a custom message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public CustomerAlreadyExistsException(String message) {
        super(message);
    }
}