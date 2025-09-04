package com.example.store.mapper;

import com.example.store.dto.OrderProductDTO;
import com.example.store.dto.ProductDTO;
import com.example.store.entity.Order;
import com.example.store.entity.Product;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "orderIds", expression = "java(product.getOrders().stream().map(Order::getId).toList())")
    ProductDTO productToProductDTO(Product product);

    List<ProductDTO> productsToProductDTOs(List<Product> products);

    OrderProductDTO productToOrderProductDTO(Product product);

    List<OrderProductDTO> productsToOrderProductDTOs(List<Product> products);
}