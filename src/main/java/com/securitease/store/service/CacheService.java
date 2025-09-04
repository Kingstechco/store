package com.securitease.store.service;

import com.securitease.store.config.CacheConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Service for manual cache management operations.
 *
 * <p>This service provides methods for manual cache eviction, cache statistics, and complex cache operations that
 * cannot be handled by simple annotations. It's particularly useful for scenarios where conditional cache eviction or
 * bulk operations are needed.
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CacheService {

    private final CacheManager cacheManager;

    /**
     * Evicts a specific customer from cache.
     *
     * @param customerId the customer ID to evict
     */
    public void evictCustomer(Long customerId) {
        if (customerId != null) {
            Objects.requireNonNull(cacheManager.getCache(CacheConfig.CUSTOMERS_CACHE))
                    .evict(customerId);
            log.debug("Evicted customer {} from cache", customerId);
        }
    }

    /**
     * Evicts customer orders cache for a specific customer.
     *
     * @param customerId the customer ID whose orders to evict
     */
    public void evictCustomerOrders(Long customerId) {
        if (customerId != null) {
            Objects.requireNonNull(cacheManager.getCache(CacheConfig.ORDER_BY_CUSTOMER_CACHE))
                    .evict(customerId);
            log.debug("Evicted orders cache for customer {}", customerId);
        }
    }

    /**
     * Evicts a specific order from cache.
     *
     * @param orderId the order ID to evict
     */
    public void evictOrder(Long orderId) {
        if (orderId != null) {
            Objects.requireNonNull(cacheManager.getCache(CacheConfig.ORDERS_CACHE))
                    .evict(orderId);
            log.debug("Evicted order {} from cache", orderId);
        }
    }

    /** Clears all customer search results cache. Useful when customer data changes that might affect search results. */
    public void evictAllCustomerSearches() {
        Objects.requireNonNull(cacheManager.getCache(CacheConfig.CUSTOMER_SEARCH_CACHE))
                .clear();
        log.debug("Cleared all customer search cache");
    }

    /**
     * Clears all caches. Use with caution in production. This should typically only be used in development or emergency
     * situations.
     */
    public void evictAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
            log.info("Cleared cache: {}", cacheName);
        });
        log.warn("All caches have been cleared");
    }

    /**
     * Warms up the cache by pre-loading frequently accessed data. This method can be called during application startup
     * or scheduled maintenance.
     *
     * @implNote In a real implementation, this would load top customers, recent orders, etc.
     */
    public void warmupCache() {
        log.info("Cache warmup initiated");
        // Implementation would depend on business requirements
        // Example: pre-load top 100 customers, recent orders, etc.
        log.info("Cache warmup completed");
    }

    /**
     * Gets cache statistics for monitoring purposes.
     *
     * @param cacheName the name of the cache to get statistics for
     * @return cache statistics as string (implementation depends on cache provider)
     */
    public String getCacheStats(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            // Redis cache statistics would be retrieved here
            return String.format("Cache %s is active", cacheName);
        }
        return String.format("Cache %s not found", cacheName);
    }
}
