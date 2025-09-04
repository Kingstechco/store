package com.example.store.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis cache configuration for the Store application.
 * <p>
 * This configuration sets up Redis as the caching provider with optimized
 * serialization, TTL settings, and cache-specific configurations. It enables
 * transparent caching of frequently accessed data to improve application
 * performance by reducing database queries.
 * </p>
 * <p>
 * Key Features:
 * <ul>
 *   <li>JSON serialization for human-readable cache entries</li>
 *   <li>Configurable TTL per cache</li>
 *   <li>Type-safe serialization with Jackson</li>
 *   <li>Optimized for DTO objects</li>
 * </ul>
 * </p>
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Cache names used throughout the application.
     */
    public static final String CUSTOMERS_CACHE = "customers";
    public static final String ORDERS_CACHE = "orders";
    public static final String CUSTOMER_SEARCH_CACHE = "customer-search";
    public static final String ORDER_BY_CUSTOMER_CACHE = "orders-by-customer";
    public static final String PRODUCTS_CACHE = "products";
    public static final String PRODUCT_SEARCH_CACHE = "product-search";

    /**
     * Configures the default Redis cache configuration.
     *
     * @return RedisCacheConfiguration with optimized settings
     */
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(), 
                ObjectMapper.DefaultTyping.NON_FINAL, 
                JsonTypeInfo.As.PROPERTY);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // Default TTL of 30 minutes
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)))
                .disableCachingNullValues(); // Don't cache null values
    }

    /**
     * Customizes cache configurations for specific caches.
     *
     * @return RedisCacheManagerBuilderCustomizer with cache-specific settings
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> builder
                // Individual customer/order records - cache longer as they change less frequently
                .withCacheConfiguration(CUSTOMERS_CACHE,
                        cacheConfiguration().entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration(ORDERS_CACHE,
                        cacheConfiguration().entryTtl(Duration.ofHours(1)))
                
                // Search results - cache shorter as they're more dynamic
                .withCacheConfiguration(CUSTOMER_SEARCH_CACHE,
                        cacheConfiguration().entryTtl(Duration.ofMinutes(15)))
                
                // Relationship queries - medium cache time
                .withCacheConfiguration(ORDER_BY_CUSTOMER_CACHE,
                        cacheConfiguration().entryTtl(Duration.ofMinutes(30)))
                
                // Product caches - similar to customer/order caching strategy
                .withCacheConfiguration(PRODUCTS_CACHE,
                        cacheConfiguration().entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration(PRODUCT_SEARCH_CACHE,
                        cacheConfiguration().entryTtl(Duration.ofMinutes(15)));
    }

    /**
     * Configures RedisTemplate for manual Redis operations if needed.
     *
     * @param connectionFactory Redis connection factory
     * @return configured RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        
        Jackson2JsonRedisSerializer<Object> serializer = 
                new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        template.afterPropertiesSet();
        return template;
    }
}