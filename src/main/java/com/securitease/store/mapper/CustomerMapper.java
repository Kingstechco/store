package com.securitease.store.mapper;

import com.securitease.store.dto.CustomerDTO;
import com.securitease.store.entity.Customer;

import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    CustomerDTO customerToCustomerDTO(Customer customer);

    List<CustomerDTO> customersToCustomerDTOs(List<Customer> customer);
}
