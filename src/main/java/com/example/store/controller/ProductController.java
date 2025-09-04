package com.example.store.controller;

import com.example.store.dto.ProductDTO;
import com.example.store.dto.ProductRequest;
import com.example.store.exception.BusinessRuleViolationException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.service.ProductService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller for managing product resources.
 * <p>
 * This controller provides HTTP endpoints for product operations including
 * creation, retrieval, updating, and deletion. It supports both individual
 * product operations and batch operations with pagination. Search functionality
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
 * @see ProductService
 * @see ProductDTO
 * @see ProductRequest
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Retrieves all products or searches products by description.
     * <p>
     * If the 'description' parameter is provided, performs a case-insensitive search
     * for products whose descriptions contain the specified string. Otherwise,
     * returns all products in the system with their associated order IDs.
     * </p>
     *
     * @param description optional search parameter to filter products by description
     * @return ResponseEntity containing a list of matching product DTOs with order IDs
     */
    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts(@RequestParam(required = false) String description) {

        List<ProductDTO> products;
        if (description != null && !description.trim().isEmpty()) {
            products = productService.findProductsByDescriptionContaining(description.trim());
        } else {
            products = productService.getAllProducts();
        }

        return ResponseEntity.ok(products);
    }

    /**
     * Retrieves products with pagination support.
     *
     * @param pageable pagination information including page number, size, and sorting
     * @return ResponseEntity containing a page of product DTOs
     */
    @GetMapping("/paged")
    public ResponseEntity<Page<ProductDTO>> getProductsPaged(Pageable pageable) {
        // Validate pagination parameters
        if (pageable.getPageSize() > 100) {
            throw new BusinessRuleViolationException("INVALID_PAGE_SIZE", 
                "Page size cannot exceed 100 items");
        }
        
        Page<ProductDTO> products = productService.getProducts(pageable);
        return ResponseEntity.ok(products);
    }

    /**
     * Retrieves a specific product by its ID.
     *
     * @param id the unique identifier of the product
     * @return ResponseEntity containing the product DTO with order IDs
     * @throws ResourceNotFoundException if product with the given ID is not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        ProductDTO product = productService
                .getProductById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return ResponseEntity.ok(product);
    }

    /**
     * Creates a new product.
     *
     * @param request the product creation request containing product details
     * @param httpRequest the HTTP servlet request used to build the location header
     * @return ResponseEntity containing the created product DTO with HTTP 201 status and Location header
     * @throws jakarta.validation.ConstraintViolationException if request validation fails
     */
    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductRequest request, 
                                                   HttpServletRequest httpRequest) {
        ProductDTO product = productService.createProduct(request);
        
        URI location = ServletUriComponentsBuilder
                .fromRequestUri(httpRequest)
                .path("/{id}")
                .buildAndExpand(product.getId())
                .toUri();
                
        return ResponseEntity.created(location).body(product);
    }

    /**
     * Updates an existing product's information.
     *
     * @param id the unique identifier of the product to update
     * @param request the product update request containing new details
     * @return ResponseEntity containing the updated product DTO
     * @throws ResourceNotFoundException if product with the given ID is not found
     * @throws jakarta.validation.ConstraintViolationException if request validation fails
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        ProductDTO product = productService.updateProduct(id, request);
        return ResponseEntity.ok(product);
    }

    /**
     * Deletes a product from the system.
     * <p>
     * This operation will remove the product from all orders that contain it.
     * </p>
     *
     * @param id the unique identifier of the product to delete
     * @return ResponseEntity with HTTP 204 No Content status
     * @throws ResourceNotFoundException if product with the given ID is not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}