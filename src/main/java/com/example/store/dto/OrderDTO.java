package com.example.store.dto;

import lombok.Data;

/**
 * Data Transfer Object (DTO) representing an order.
 * <p>
 * This DTO is used to transfer order data between different layers of the application,
 * particularly between the service layer and the presentation layer. It includes
 * order information along with associated customer data to provide a complete view
 * of the order's context.
 * </p>
 * <p>
 * The customer field contains simplified customer information ({@link OrderCustomerDTO})
 * to avoid circular references and reduce data transfer overhead.
 * </p>
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 * @see OrderCustomerDTO
 * @see com.example.store.entity.Order
 */
@Data
public class OrderDTO {
    private Long id;
    private String description;
    private OrderCustomerDTO customer;
}
