package com.securitease.store.repository;

import com.securitease.store.entity.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Find products with their orders eagerly loaded to avoid N+1 queries. */
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.orders WHERE p.description LIKE %:description%")
    List<Product> findByDescriptionContainingIgnoreCaseWithOrders(@Param("description") String description);

    /** Find a product by ID with orders eagerly loaded. */
    @EntityGraph(attributePaths = {"orders"})
    Optional<Product> findWithOrdersById(Long id);

    /** Find all products with pagination and eager loading of orders. */
    @EntityGraph(attributePaths = {"orders"})
    Page<Product> findAll(Pageable pageable);

    /** Find products by description containing the given string (case-insensitive). */
    List<Product> findByDescriptionContainingIgnoreCase(String description);
}
