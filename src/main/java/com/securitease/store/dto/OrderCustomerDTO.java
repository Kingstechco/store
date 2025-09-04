package com.securitease.store.dto;

import lombok.Data;

/**
 * Simplified DTO representing a customer within an order context.
 *
 * <p>This DTO is used when including customer information as part of an order response to avoid circular references and
 * reduce data transfer overhead. It contains only the essential customer information needed for order-related
 * operations.
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 * @see OrderDTO
 * @see CustomerDTO
 */
@Data
public class OrderCustomerDTO {
    private Long id;
    private String name;
}
