package com.securitease.store.controller;

import com.securitease.store.dto.OrderDTO;
import com.securitease.store.dto.OrderRequest;
import com.securitease.store.exception.BusinessRuleViolationException;
import com.securitease.store.exception.ResourceNotFoundException;
import com.securitease.store.service.OrderService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller for managing order resources.
 *
 * <p>This controller provides HTTP endpoints for CRUD order operations. It handles the relationship between orders and
 * customers, ensuring data integrity and proper validation. Supports pagination and customer-specific order retrieval.
 *
 * @author Musa Maringa
 * @version 1.0
 * @since 1.0
 * @see OrderService
 * @see OrderDTO
 * @see OrderRequest
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    public static final String INVALID_PAGE_SIZE = "INVALID_PAGE_SIZE";
    private final OrderService orderService;

    /**
     * Retrieves all orders in the system.
     *
     * @return ResponseEntity containing a list of all order DTOs
     */
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        List<OrderDTO> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Retrieves orders with pagination support.
     *
     * @param pageable pagination information including page number, size, and sorting
     * @return ResponseEntity containing a page of order DTOs
     */
    @GetMapping("/getOrders")
    public ResponseEntity<Page<OrderDTO>> getOrders(Pageable pageable) {
        // Validate pagination parameters
        if (pageable.getPageSize() > 100) {
            throw new BusinessRuleViolationException(INVALID_PAGE_SIZE, "Page size cannot exceed 100 items");
        }

        Page<OrderDTO> orders = orderService.getOrders(pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * Retrieves a specific order by its ID.
     *
     * @param id the unique identifier of the order
     * @return ResponseEntity containing the order DTO
     * @throws ResourceNotFoundException if the order with the given ID is not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        OrderDTO order =
                orderService.getOrderById(id).orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        return ResponseEntity.ok(order);
    }

    /**
     * Retrieves all orders associated with a specific customer.
     *
     * @param customerId the unique identifier of the customer
     * @return ResponseEntity containing a list of order DTOs belonging to the customer
     */
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<List<OrderDTO>> getOrdersByCustomerId(@PathVariable Long customerId) {
        List<OrderDTO> orders = orderService.getOrdersByCustomerId(customerId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Creates a new order.
     *
     * <p>The order will be associated with the customer specified in the request.
     *
     * @param request the order creation request containing order details and customer ID
     * @param httpRequest the HTTP servlet request used to build the location header
     * @return ResponseEntity containing the created order DTO with HTTP 201 status and Location header
     * @throws ResourceNotFoundException if the specified customer is not found
     * @throws jakarta.validation.ConstraintViolationException if request validation fails
     */
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(
            @Valid @RequestBody OrderRequest request, HttpServletRequest httpRequest) {
        OrderDTO order = orderService.createOrder(request);

        URI location = ServletUriComponentsBuilder.fromRequestUri(httpRequest)
                .path("/{id}")
                .buildAndExpand(order.getId())
                .toUri();

        return ResponseEntity.created(location).body(order);
    }

    /**
     * Updates an existing order's information.
     *
     * <p>This can include changing the order description and/or reassigning it to a different customer.
     *
     * @param id the unique identifier of the order to update
     * @param request the order update request containing new details
     * @return ResponseEntity containing the updated order DTO
     * @throws ResourceNotFoundException if an order or customer is not found
     * @throws jakarta.validation.ConstraintViolationException if request validation fails
     */
    @PutMapping("/{id}")
    public ResponseEntity<OrderDTO> updateOrder(@PathVariable Long id, @Valid @RequestBody OrderRequest request) {
        OrderDTO order = orderService.updateOrder(id, request);
        return ResponseEntity.ok(order);
    }

    /**
     * Deletes an order from the system.
     *
     * @param id the unique identifier of the order to delete
     * @return ResponseEntity with HTTP 204 No Content status
     * @throws ResourceNotFoundException if the order with the given ID is not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
