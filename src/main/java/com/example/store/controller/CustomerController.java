package com.example.store.controller;

import com.example.store.dto.CustomerDTO;
import com.example.store.dto.CustomerRequest;
import com.example.store.exception.BusinessRuleViolationException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.service.CustomerService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;

/**
 * REST controller for managing customer resources.
 * <p>
 * This controller provides HTTP endpoints for customer operations including
 * creation, retrieval, updating, and deletion. It supports both individual
 * customer operations and batch operations with pagination. Search functionality
 * is available through query parameters.
 * </p>
 * <p>
 * All endpoints return appropriate HTTP status codes and use proper error handling
 * through the global exception handler. Input validation is performed using
 * Bean Validation annotations.
 * </p>
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 * @see CustomerService
 * @see CustomerDTO
 * @see CustomerRequest
 */
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Retrieves all customers or searches customers by name.
     * <p>
     * If the 'name' parameter is provided, performs a case-insensitive search
     * for customers whose names contain the specified string. Otherwise,
     * returns all customers in the system.
     * </p>
     *
     * @param name optional search parameter to filter customers by name
     * @return ResponseEntity containing a list of matching customer DTOs
     */
    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAllCustomers(@RequestParam(required = false) String name) {

        List<CustomerDTO> customers;
        if (name != null && !name.trim().isEmpty()) {
            customers = customerService.findCustomersByNameContaining(name.trim());
        } else {
            customers = customerService.getAllCustomers();
        }

        return ResponseEntity.ok(customers);
    }

    /**
     * Retrieves customers with pagination support.
     *
     * @param pageable pagination information including page number, size, and sorting
     * @return ResponseEntity containing a page of customer DTOs
     */
    @GetMapping("/paged")
    public ResponseEntity<Page<CustomerDTO>> getCustomersPaged(Pageable pageable) {
        // Validate pagination parameters
        if (pageable.getPageSize() > 100) {
            throw new BusinessRuleViolationException("INVALID_PAGE_SIZE", 
                "Page size cannot exceed 100 items");
        }
        
        Page<CustomerDTO> customers = customerService.getCustomers(pageable);
        return ResponseEntity.ok(customers);
    }

    /**
     * Retrieves a specific customer by their ID.
     *
     * @param id the unique identifier of the customer
     * @return ResponseEntity containing the customer DTO
     * @throws ResourceNotFoundException if customer with the given ID is not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable Long id) {
        CustomerDTO customer = customerService
                .getCustomerById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
        return ResponseEntity.ok(customer);
    }

    /**
     * Creates a new customer.
     *
     * @param request the customer creation request containing customer details
     * @param httpRequest the HTTP servlet request used to build the location header
     * @return ResponseEntity containing the created customer DTO with HTTP 201 status and Location header
     * @throws jakarta.validation.ConstraintViolationException if request validation fails
     */
    @PostMapping
    public ResponseEntity<CustomerDTO> createCustomer(@Valid @RequestBody CustomerRequest request, 
                                                     HttpServletRequest httpRequest) {
        CustomerDTO customer = customerService.createCustomer(request);
        
        URI location = ServletUriComponentsBuilder
                .fromRequestUri(httpRequest)
                .path("/{id}")
                .buildAndExpand(customer.getId())
                .toUri();
                
        return ResponseEntity.created(location).body(customer);
    }

    /**
     * Updates an existing customer's information.
     *
     * @param id the unique identifier of the customer to update
     * @param request the customer update request containing new details
     * @return ResponseEntity containing the updated customer DTO
     * @throws ResourceNotFoundException if customer with the given ID is not found
     * @throws jakarta.validation.ConstraintViolationException if request validation fails
     */
    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> updateCustomer(
            @PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        CustomerDTO customer = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(customer);
    }

    /**
     * Deletes a customer from the system.
     * <p>
     * This operation will cascade delete all orders associated with the customer.
     * </p>
     *
     * @param id the unique identifier of the customer to delete
     * @return ResponseEntity with HTTP 204 No Content status
     * @throws ResourceNotFoundException if customer with the given ID is not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
