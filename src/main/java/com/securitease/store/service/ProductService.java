package com.example.store.service;

import com.example.store.dto.ProductDTO;
import com.example.store.dto.ProductRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing products.
 * <p>
 * This service provides business logic operations for product management,
 * including CRUD operations and search functionality. It handles the
 * relationship between products and orders while maintaining data integrity.
 * </p>
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 */
public interface ProductService {

    /**
     * Retrieves all products from the system.
     *
     * @return a list of all product DTOs with order IDs, or an empty list if no products exist
     */
    List<ProductDTO> getAllProducts();

    /**
     * Retrieves products with pagination support.
     *
     * @param pageable the pagination information including page number, size, and sorting
     * @return a page of product DTOs
     * @throws IllegalArgumentException if pageable is null
     */
    Page<ProductDTO> getProducts(Pageable pageable);

    /**
     * Retrieves a product by its unique identifier.
     *
     * @param id the unique identifier of the product
     * @return an Optional containing the product DTO if found, or empty if not found
     * @throws IllegalArgumentException if id is null
     */
    Optional<ProductDTO> getProductById(Long id);

    /**
     * Searches for products by description containing the specified text.
     *
     * @param description the description text to search for (case-insensitive)
     * @return a list of matching product DTOs, or an empty list if none found
     * @throws IllegalArgumentException if description is null or empty
     */
    List<ProductDTO> findProductsByDescriptionContaining(String description);

    /**
     * Creates a new product in the system.
     *
     * @param request the product creation request containing product details
     * @return the created product DTO with assigned ID and empty order list
     * @throws IllegalArgumentException if request is null or contains invalid data
     * @throws jakarta.validation.ConstraintViolationException if request fails validation
     */
    ProductDTO createProduct(ProductRequest request);

    /**
     * Updates an existing product's information.
     *
     * @param id the unique identifier of the product to update
     * @param request the product update request containing new product details
     * @return the updated product DTO
     * @throws com.example.store.exception.ResourceNotFoundException if product is not found
     * @throws IllegalArgumentException if id or request is null, or request contains invalid data
     * @throws jakarta.validation.ConstraintViolationException if request fails validation
     */
    ProductDTO updateProduct(Long id, ProductRequest request);

    /**
     * Deletes a product from the system.
     * <p>
     * This operation will remove the product from all orders that contain it.
     * </p>
     *
     * @param id the unique identifier of the product to delete
     * @throws com.example.store.exception.ResourceNotFoundException if product with given ID is not found
     * @throws IllegalArgumentException if id is null
     */
    void deleteProduct(Long id);
}