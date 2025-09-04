package com.securitease.store.dto;

import lombok.Data;

import java.util.List;

/**
 * Data Transfer Object (DTO) representing a product.
 *
 * <p>This DTO is used to transfer product data between different layers of the application, particularly between the
 * service layer and the presentation layer. It includes product information along with the list of order IDs that
 * contain this product.
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 * @see com.securitease.store.entity.Product
 */
@Data
public class ProductDTO {
    private Long id;
    private String description;
    private List<Long> orderIds;
}
