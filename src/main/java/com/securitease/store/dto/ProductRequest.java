package com.securitease.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * Request DTO for product operations.
 *
 * <p>This DTO is used for both creating new products and updating existing ones. It contains all the necessary
 * information required for product operations with proper validation constraints to ensure data integrity.
 *
 * <p>Validation rules:
 *
 * <ul>
 *   <li>Description is required and cannot be blank
 *   <li>Description must be between 2 and 255 characters
 * </ul>
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 * @see ProductDTO
 * @see com.securitease.store.entity.Product
 */
@Data
public class ProductRequest {
    @NotBlank(message = "Product description is required")
    @Size(min = 2, max = 255, message = "Product description must be between 2 and 255 characters")
    private String description;
}
