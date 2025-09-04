package com.example.store.controller;

import com.example.store.service.CacheService;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for cache management operations.
 * <p>
 * This controller provides administrative endpoints for cache management
 * including cache eviction, statistics, and warmup operations. These endpoints
 * are typically used by administrators or monitoring systems.
 * </p>
 * <p>
 * Note: This controller is only enabled when cache management is explicitly
 * enabled via configuration to prevent accidental exposure in production.
 * </p>
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/admin/cache")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.cache.management.enabled", havingValue = "true", matchIfMissing = false)
public class CacheController {

    private final CacheService cacheService;

    /**
     * Evicts a specific customer from cache.
     *
     * @param customerId the customer ID to evict
     * @return success response
     */
    @DeleteMapping("/customers/{customerId}")
    public ResponseEntity<String> evictCustomer(@PathVariable Long customerId) {
        cacheService.evictCustomer(customerId);
        return ResponseEntity.ok("Customer " + customerId + " evicted from cache");
    }

    /**
     * Evicts customer orders cache for a specific customer.
     *
     * @param customerId the customer ID whose orders to evict
     * @return success response
     */
    @DeleteMapping("/customers/{customerId}/orders")
    public ResponseEntity<String> evictCustomerOrders(@PathVariable Long customerId) {
        cacheService.evictCustomerOrders(customerId);
        return ResponseEntity.ok("Orders cache for customer " + customerId + " evicted");
    }

    /**
     * Evicts a specific order from cache.
     *
     * @param orderId the order ID to evict
     * @return success response
     */
    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<String> evictOrder(@PathVariable Long orderId) {
        cacheService.evictOrder(orderId);
        return ResponseEntity.ok("Order " + orderId + " evicted from cache");
    }

    /**
     * Clears all customer search results cache.
     *
     * @return success response
     */
    @DeleteMapping("/searches/customers")
    public ResponseEntity<String> evictCustomerSearches() {
        cacheService.evictAllCustomerSearches();
        return ResponseEntity.ok("Customer search cache cleared");
    }

    /**
     * Clears all caches. Use with extreme caution.
     *
     * @return success response
     */
    @DeleteMapping("/all")
    public ResponseEntity<String> evictAllCaches() {
        cacheService.evictAllCaches();
        return ResponseEntity.ok("All caches cleared");
    }

    /**
     * Warms up caches with frequently accessed data.
     *
     * @return success response
     */
    @PostMapping("/warmup")
    public ResponseEntity<String> warmupCache() {
        cacheService.warmupCache();
        return ResponseEntity.ok("Cache warmup initiated");
    }

    /**
     * Gets cache statistics for a specific cache.
     *
     * @param cacheName the name of the cache
     * @return cache statistics
     */
    @GetMapping("/stats/{cacheName}")
    public ResponseEntity<String> getCacheStats(@PathVariable String cacheName) {
        String stats = cacheService.getCacheStats(cacheName);
        return ResponseEntity.ok(stats);
    }
}