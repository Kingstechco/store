package com.securitease.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * Request DTO for customer operations.
 *
 * <p>This DTO is used for both creating new customers and updating existing ones. It contains all the necessary
 * information required for customer operations with proper validation constraints to ensure data integrity.
 *
 * <p>Validation rules:
 *
 * <ul>
 *   <li>Name is required and cannot be blank
 *   <li>Name must be between 2 and 255 characters
 * </ul>
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 * @see CustomerDTO
 * @see com.securitease.store.entity.Customer
 */
@Data
public class CustomerRequest {
    @NotBlank(message = "Customer name is required")
    @Size(min = 2, max = 255, message = "Customer name must be between 2 and 255 characters")
    private String name;
}
