package com.securitease.store.mapper;

import com.securitease.store.dto.OrderCustomerDTO;
import com.securitease.store.dto.OrderDTO;
import com.securitease.store.entity.Customer;
import com.securitease.store.entity.Order;

import org.mapstruct.Mapper;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {ProductMapper.class})
public interface OrderMapper {
    OrderDTO orderToOrderDTO(Order order);

    List<OrderDTO> ordersToOrderDTOs(List<Order> orders);

    OrderCustomerDTO orderToOrderCustomerDTO(Customer customer);
}
