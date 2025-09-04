package com.securitease.store.service;

import com.securitease.store.dto.OrderDTO;
import com.securitease.store.dto.OrderRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing orders.
 *
 * <p>This service provides business logic operations for order management, including CRUD operations and
 * customer-specific order retrieval. It handles the relationship between orders and customers while maintaining data
 * integrity.
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 */
public interface OrderService {

    /**
     * Retrieves all orders from the system.
     *
     * @return a list of all order DTOs, or an empty list if no orders exist
     */
    List<OrderDTO> getAllOrders();

    /**
     * Retrieves orders with pagination support.
     *
     * @param pageable the pagination information including page number, size, and sorting
     * @return a page of order DTOs
     * @throws IllegalArgumentException if pageable is null
     */
    Page<OrderDTO> getOrders(Pageable pageable);

    /**
     * Retrieves an order by its unique identifier.
     *
     * @param id the unique identifier of the order
     * @return an Optional containing the order DTO if found, or empty if not found
     * @throws IllegalArgumentException if id is null
     */
    Optional<OrderDTO> getOrderById(Long id);

    /**
     * Retrieves all orders associated with a specific customer.
     *
     * @param customerId the unique identifier of the customer
     * @return a list of order DTOs belonging to the specified customer, or an empty list if none found
     * @throws IllegalArgumentException if customerId is null
     */
    List<OrderDTO> getOrdersByCustomerId(Long customerId);

    /**
     * Creates a new order in the system. The order will be associated with the customer specified in the request.
     *
     * @param request the order creation request containing order details and customer ID
     * @return the created order DTO with assigned ID
     * @throws com.securitease.store.exception.ResourceNotFoundException if the specified customer is not found
     * @throws IllegalArgumentException if request is null or contains invalid data
     * @throws jakarta.validation.ConstraintViolationException if request fails validation
     */
    OrderDTO createOrder(OrderRequest request);

    /**
     * Updates an existing order's information. This can include changing the order description and/or reassigning it to
     * a different customer.
     *
     * @param id the unique identifier of the order to update
     * @param request the order update request containing new order details
     * @return the updated order DTO
     * @throws com.securitease.store.exception.ResourceNotFoundException if order or customer is not found
     * @throws IllegalArgumentException if id or request is null, or request contains invalid data
     * @throws jakarta.validation.ConstraintViolationException if request fails validation
     */
    OrderDTO updateOrder(Long id, OrderRequest request);

    /**
     * Deletes an order from the system.
     *
     * @param id the unique identifier of the order to delete
     * @throws com.securitease.store.exception.ResourceNotFoundException if order with given ID is not found
     * @throws IllegalArgumentException if id is null
     */
    void deleteOrder(Long id);
}
