package com.example.store.service;

import com.example.store.dto.CustomerDTO;
import com.example.store.dto.CustomerRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing customers.
 * <p>
 * This service provides business logic operations for customer management,
 * including CRUD operations and search functionality. It serves as an abstraction
 * layer between the controller and data access layers.
 * </p>
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 */
public interface CustomerService {

    /**
     * Retrieves all customers from the system.
     *
     * @return a list of all customer DTOs, or an empty list if no customers exist
     */
    List<CustomerDTO> getAllCustomers();

    /**
     * Retrieves customers with pagination support.
     *
     * @param pageable the pagination information including page number, size, and sorting
     * @return a page of customer DTOs
     * @throws IllegalArgumentException if pageable is null
     */
    Page<CustomerDTO> getCustomers(Pageable pageable);

    /**
     * Retrieves a customer by their unique identifier.
     *
     * @param id the unique identifier of the customer
     * @return an Optional containing the customer DTO if found, or empty if not found
     * @throws IllegalArgumentException if id is null
     */
    Optional<CustomerDTO> getCustomerById(Long id);

    /**
     * Searches for customers whose names contain the specified query string.
     * The search is case-insensitive and matches partial names.
     *
     * @param nameQuery the search query string to match against customer names
     * @return a list of customer DTOs matching the search criteria, or an empty list if none found
     * @throws IllegalArgumentException if nameQuery is null or empty
     */
    List<CustomerDTO> findCustomersByNameContaining(String nameQuery);

    /**
     * Creates a new customer in the system.
     *
     * @param request the customer creation request containing customer details
     * @return the created customer DTO with assigned ID
     * @throws IllegalArgumentException if request is null or contains invalid data
     * @throws jakarta.validation.ConstraintViolationException if request fails validation
     */
    CustomerDTO createCustomer(CustomerRequest request);

    /**
     * Updates an existing customer's information.
     *
     * @param id the unique identifier of the customer to update
     * @param request the customer update request containing new customer details
     * @return the updated customer DTO
     * @throws com.example.store.exception.ResourceNotFoundException if customer with given ID is not found
     * @throws IllegalArgumentException if id or request is null, or request contains invalid data
     * @throws jakarta.validation.ConstraintViolationException if request fails validation
     */
    CustomerDTO updateCustomer(Long id, CustomerRequest request);

    /**
     * Deletes a customer from the system.
     * This operation will also cascade delete all associated orders.
     *
     * @param id the unique identifier of the customer to delete
     * @throws com.example.store.exception.ResourceNotFoundException if customer with given ID is not found
     * @throws IllegalArgumentException if id is null
     */
    void deleteCustomer(Long id);
}
