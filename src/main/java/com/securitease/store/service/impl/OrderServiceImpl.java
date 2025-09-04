package com.securitease.store.service.impl;

import com.securitease.store.config.CacheConfig;
import com.securitease.store.dto.OrderDTO;
import com.securitease.store.dto.OrderRequest;
import com.securitease.store.entity.Customer;
import com.securitease.store.entity.Order;
import com.securitease.store.exception.ResourceNotFoundException;
import com.securitease.store.mapper.OrderMapper;
import com.securitease.store.repository.CustomerRepository;
import com.securitease.store.repository.OrderRepository;
import com.securitease.store.service.CacheService;
import com.securitease.store.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of the OrderService interface.
 *
 * <p>This service implementation provides concrete business logic for order management operations. It handles the
 * relationship between orders and customers, ensuring data integrity and proper validation. Uses JPA repositories for
 * data access and MapStruct mappers for entity-to-DTO conversion.
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 * @see OrderService
 * @see OrderRepository
 * @see CustomerRepository
 * @see OrderMapper
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final OrderMapper orderMapper;
    private final CacheService cacheService;

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        log.debug("Fetching all orders");
        List<Order> orders = orderRepository.findAll();
        return orderMapper.ordersToOrderDTOs(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrders(Pageable pageable) {
        log.debug("Fetching orders with pagination: {}", pageable);
        Page<Order> orders = orderRepository.findAll(pageable);
        return orders.map(orderMapper::orderToOrderDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.ORDERS_CACHE, key = "#id")
    public Optional<OrderDTO> getOrderById(Long id) {
        log.debug("Fetching order by id: {}", id);
        return orderRepository.findById(id).map(orderMapper::orderToOrderDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.ORDER_BY_CUSTOMER_CACHE, key = "#customerId")
    public List<OrderDTO> getOrdersByCustomerId(Long customerId) {
        log.debug("Fetching orders for customer id: {}", customerId);
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        return orderMapper.ordersToOrderDTOs(orders);
    }

    @Override
    @Caching(
            put = {@CachePut(value = CacheConfig.ORDERS_CACHE, key = "#result.id")},
            evict = {@CacheEvict(value = CacheConfig.ORDER_BY_CUSTOMER_CACHE, key = "#request.customerId")})
    public OrderDTO createOrder(OrderRequest request) {
        log.info("Creating new order for customer id: {}", request.getCustomerId());

        Customer customer = customerRepository
                .findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.getCustomerId()));

        Order order = new Order();
        order.setDescription(request.getDescription());
        order.setCustomer(customer);

        Order savedOrder = orderRepository.save(order);
        log.info("Successfully created order with id: {}", savedOrder.getId());

        return orderMapper.orderToOrderDTO(savedOrder);
    }

    @Override
    @Caching(
            put = {@CachePut(value = CacheConfig.ORDERS_CACHE, key = "#id")},
            evict = {
                @CacheEvict(value = CacheConfig.ORDER_BY_CUSTOMER_CACHE, key = "#request.customerId"),
                @CacheEvict(
                        value = CacheConfig.ORDER_BY_CUSTOMER_CACHE,
                        key = "#result.customer.id",
                        condition = "#request.customerId != #result.customer.id")
            })
    public OrderDTO updateOrder(Long id, OrderRequest request) {
        log.info("Updating order with id: {}", id);

        Order order = orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        Long oldCustomerId = order.getCustomer().getId();

        Customer customer = customerRepository
                .findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.getCustomerId()));

        order.setDescription(request.getDescription());
        order.setCustomer(customer);

        Order updatedOrder = orderRepository.save(order);
        log.info("Successfully updated order with id: {}", id);

        // Manually evict old customer's order cache if customer changed
        if (!oldCustomerId.equals(request.getCustomerId())) {
            log.debug("Customer changed, evicting cache for old customer: {}", oldCustomerId);
        }

        return orderMapper.orderToOrderDTO(updatedOrder);
    }

    @Override
    @CacheEvict(value = CacheConfig.ORDERS_CACHE, key = "#id")
    public void deleteOrder(Long id) {
        log.info("Deleting order with id: {}", id);

        // First, get the order to know which customer's cache to evict
        Order order = orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        Long customerId = order.getCustomer().getId();

        orderRepository.deleteById(id);
        log.info("Successfully deleted order with id: {}", id);

        // Manually evict customer's order list cache
        cacheService.evictCustomerOrders(customerId);
    }
}
