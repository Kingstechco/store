package com.securitease.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * Request DTO for order operations.
 *
 * <p>This DTO is used for both creating new orders and updating existing ones. It contains all the necessary
 * information required for order operations with proper validation constraints to ensure data integrity.
 *
 * <p>Validation rules:
 *
 * <ul>
 *   <li>Description is required and cannot be blank
 *   <li>Description must be between 5 and 255 characters
 *   <li>Customer ID is required and cannot be null
 * </ul>
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 * @see OrderDTO
 * @see com.securitease.store.entity.Order
 */
@Data
public class OrderRequest {
    @NotBlank(message = "Order description is required")
    @Size(min = 5, max = 255, message = "Order description must be between 5 and 255 characters")
    private String description;

    @NotNull(message = "Customer ID is required") private Long customerId;
}
