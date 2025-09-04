package com.securitease.store.dto;

import lombok.Data;

import java.util.List;

/**
 * Data Transfer Object (DTO) representing a customer.
 *
 * <p>This DTO is used to transfer customer data between different layers of the application, particularly between the
 * service layer and the presentation layer. It includes customer information along with associated orders to provide a
 * complete view of the customer's data.
 *
 * <p>The orders list contains simplified order information ({@link CustomerOrderDTO}) to avoid circular references and
 * reduce data transfer overhead.
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 * @see CustomerOrderDTO
 * @see com.securitease.store.entity.Customer
 */
@Data
public class CustomerDTO {
    private Long id;
    private String name;
    private List<CustomerOrderDTO> orders;
}
