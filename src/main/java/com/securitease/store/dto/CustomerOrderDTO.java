package com.securitease.store.dto;

import lombok.Data;

/**
 * Simplified DTO representing an order within a customer context.
 *
 * <p>This DTO is used when including order information as part of a customer response to avoid circular references and
 * reduce data transfer overhead. It contains only the essential order information needed for customer-related
 * operations.
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 * @see CustomerDTO
 * @see OrderDTO
 */
@Data
public class CustomerOrderDTO {
    private Long id;
    private String description;
}
