package com.securitease.store.mapper;

import com.securitease.store.dto.OrderProductDTO;
import com.securitease.store.dto.ProductDTO;
import com.securitease.store.entity.Product;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "orderIds", source = "orders")
    ProductDTO productToProductDTO(Product product);

    List<ProductDTO> productsToProductDTOs(List<Product> products);

    OrderProductDTO productToOrderProductDTO(Product product);

    List<OrderProductDTO> productsToOrderProductDTOs(List<Product> products);

    // helper
    default List<Long> mapOrdersToIds(List<com.securitease.store.entity.Order> orders) {
        return orders == null
                ? List.of()
                : orders.stream().map(com.securitease.store.entity.Order::getId).toList();
    }
}
