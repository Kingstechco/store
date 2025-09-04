package com.example.store.service.impl;

import com.example.store.config.CacheConfig;
import com.example.store.dto.CustomerDTO;
import com.example.store.dto.CustomerRequest;
import com.example.store.entity.Customer;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.mapper.CustomerMapper;
import com.example.store.repository.CustomerRepository;
import com.example.store.service.CustomerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of the CustomerService interface.
 * <p>
 * This service implementation provides concrete business logic for customer management
 * operations. It uses JPA repositories for data access and MapStruct mappers for
 * entity-to-DTO conversion. All operations are transactional and include appropriate
 * logging for debugging and monitoring purposes.
 * </p>
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 * @see CustomerService
 * @see CustomerRepository
 * @see CustomerMapper
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDTO> getAllCustomers() {
        log.debug("Fetching all customers");
        List<Customer> customers = customerRepository.findAll();
        return customerMapper.customersToCustomerDTOs(customers);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerDTO> getCustomers(Pageable pageable) {
        log.debug("Fetching customers with pagination: {}", pageable);
        Page<Customer> customers = customerRepository.findAll(pageable);
        return customers.map(customerMapper::customerToCustomerDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CUSTOMERS_CACHE, key = "#id")
    public Optional<CustomerDTO> getCustomerById(Long id) {
        log.debug("Fetching customer by id: {}", id);
        return customerRepository.findById(id).map(customerMapper::customerToCustomerDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CUSTOMER_SEARCH_CACHE, key = "#nameQuery.toLowerCase()")
    public List<CustomerDTO> findCustomersByNameContaining(String nameQuery) {
        log.debug("Searching customers with name containing: {}", nameQuery);
        List<Customer> customers = customerRepository.findByNameContainingIgnoreCase(nameQuery);
        return customerMapper.customersToCustomerDTOs(customers);
    }

    @Override
    @CachePut(value = CacheConfig.CUSTOMERS_CACHE, key = "#result.id")
    @CacheEvict(value = CacheConfig.CUSTOMER_SEARCH_CACHE, allEntries = true)
    public CustomerDTO createCustomer(CustomerRequest request) {
        log.info("Creating new customer with name: {}", request.getName());

        Customer customer = new Customer();
        customer.setName(request.getName());

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Successfully created customer with id: {}", savedCustomer.getId());

        return customerMapper.customerToCustomerDTO(savedCustomer);
    }

    @Override
    @CachePut(value = CacheConfig.CUSTOMERS_CACHE, key = "#id")
    @CacheEvict(value = CacheConfig.CUSTOMER_SEARCH_CACHE, allEntries = true)
    public CustomerDTO updateCustomer(Long id, CustomerRequest request) {
        log.info("Updating customer with id: {}", id);

        Customer customer =
                customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        customer.setName(request.getName());
        Customer updatedCustomer = customerRepository.save(customer);

        log.info("Successfully updated customer with id: {}", id);
        return customerMapper.customerToCustomerDTO(updatedCustomer);
    }

    @Override
    @CacheEvict(value = CacheConfig.CUSTOMERS_CACHE, key = "#id")
    @CacheEvict(value = CacheConfig.CUSTOMER_SEARCH_CACHE, allEntries = true)
    @CacheEvict(value = CacheConfig.ORDER_BY_CUSTOMER_CACHE, key = "#id")
    public void deleteCustomer(Long id) {
        log.info("Deleting customer with id: {}", id);

        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Customer", "id", id);
        }

        customerRepository.deleteById(id);
        log.info("Successfully deleted customer with id: {}", id);
    }
}
