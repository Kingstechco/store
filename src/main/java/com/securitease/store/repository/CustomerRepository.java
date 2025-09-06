package com.securitease.store.repository;

import com.securitease.store.entity.Customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByNameContainingIgnoreCase(String name);

    /** Find customers with their orders eagerly loaded to avoid N+1 queries. */
    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.orders WHERE c.name LIKE %:name%")
    List<Customer> findByNameContainingIgnoreCaseWithOrders(@Param("name") String name);

    /** Find a customer by ID with orders eagerly loaded. */
    @EntityGraph(attributePaths = {"orders"})
    Optional<Customer> findWithOrdersById(Long id);

    /** Find all customers with pagination and eager loading of orders. */
    @EntityGraph(attributePaths = {"orders"})
    Page<Customer> findAll(Pageable pageable);
}
