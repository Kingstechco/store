package com.example.store.dto;

import lombok.Data;

/**
 * Simplified DTO representing a product within an order context.
 * <p>
 * This DTO is used when including product information as part of an order response
 * to avoid circular references and reduce data transfer overhead. It contains only
 * the essential product information needed for order-related operations.
 * </p>
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 * @see OrderDTO
 * @see ProductDTO
 */
@Data
public class OrderProductDTO {
    private Long id;
    private String description;
}