package com.securitease.store.testutil;

import com.securitease.store.dto.*;
import com.securitease.store.entity.Customer;
import com.securitease.store.entity.Order;
import com.securitease.store.entity.Product;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test data builder utility class for creating test objects.
 *
 * <p>This utility provides builder pattern methods for creating consistent test data across all test classes, following
 * the Test Data Builder pattern to make tests more readable and maintainable.
 */
public class TestDataBuilder {

    // Customer builders
    public static CustomerBuilder aCustomer() {
        return new CustomerBuilder();
    }

    public static CustomerDTOBuilder aCustomerDTO() {
        return new CustomerDTOBuilder();
    }

    public static CustomerRequestBuilder aCustomerRequest() {
        return new CustomerRequestBuilder();
    }

    // Order builders
    public static OrderBuilder anOrder() {
        return new OrderBuilder();
    }

    public static OrderDTOBuilder anOrderDTO() {
        return new OrderDTOBuilder();
    }

    public static OrderRequestBuilder anOrderRequest() {
        return new OrderRequestBuilder();
    }

    // Product builders
    public static ProductBuilder aProduct() {
        return new ProductBuilder();
    }

    public static ProductDTOBuilder aProductDTO() {
        return new ProductDTOBuilder();
    }

    public static ProductRequestBuilder aProductRequest() {
        return new ProductRequestBuilder();
    }

    // Customer Entity Builder
    public static class CustomerBuilder {
        private Long id = 1L;
        private String name = "John Doe";

        public CustomerBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public CustomerBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public Customer build() {
            Customer customer = new Customer(name);
            customer.setId(id);
            return customer;
        }
    }

    // Customer DTO Builder
    public static class CustomerDTOBuilder {
        private Long id = 1L;
        private String name = "John Doe";
        private List<CustomerOrderDTO> orders = new ArrayList<>();

        public CustomerDTOBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public CustomerDTOBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public CustomerDTOBuilder withOrders(CustomerOrderDTO... orders) {
            this.orders = Arrays.asList(orders);
            return this;
        }

        public CustomerDTO build() {
            CustomerDTO dto = new CustomerDTO();
            dto.setId(id);
            dto.setName(name);
            dto.setOrders(orders);
            return dto;
        }
    }

    // Customer Request Builder
    public static class CustomerRequestBuilder {
        private String name = "John Doe";

        public CustomerRequestBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public CustomerRequest build() {
            CustomerRequest request = new CustomerRequest();
            request.setName(name);
            return request;
        }
    }

    // Order Entity Builder
    public static class OrderBuilder {
        private Long id = 1L;
        private String description = "Test Order";
        private Customer customer = aCustomer().build();
        private List<Product> products = new ArrayList<>();

        public OrderBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public OrderBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public OrderBuilder withCustomer(Customer customer) {
            this.customer = customer;
            return this;
        }

        public OrderBuilder withProducts(Product... products) {
            this.products = Arrays.asList(products);
            return this;
        }

        public Order build() {
            Order order = new Order(description, customer);
            order.setId(id);
            order.setProducts(products);
            return order;
        }
    }

    // Order DTO Builder
    public static class OrderDTOBuilder {
        private Long id = 1L;
        private String description = "Test Order";
        private OrderCustomerDTO customer = anOrderCustomerDTO().build();
        private List<OrderProductDTO> products = new ArrayList<>();

        public OrderDTOBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public OrderDTOBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public OrderDTOBuilder withCustomer(OrderCustomerDTO customer) {
            this.customer = customer;
            return this;
        }

        public OrderDTOBuilder withProducts(OrderProductDTO... products) {
            this.products = Arrays.asList(products);
            return this;
        }

        public OrderDTO build() {
            OrderDTO dto = new OrderDTO();
            dto.setId(id);
            dto.setDescription(description);
            dto.setCustomer(customer);
            dto.setProducts(products);
            return dto;
        }
    }

    // Order Request Builder
    public static class OrderRequestBuilder {
        private String description = "Test Order";
        private Long customerId = 1L;

        public OrderRequestBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public OrderRequestBuilder withCustomerId(Long customerId) {
            this.customerId = customerId;
            return this;
        }

        public OrderRequest build() {
            OrderRequest request = new OrderRequest();
            request.setDescription(description);
            request.setCustomerId(customerId);
            return request;
        }
    }

    // Product Entity Builder
    public static class ProductBuilder {
        private Long id = 1L;
        private String description = "Test Product";
        private List<Order> orders = new ArrayList<>();

        public ProductBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public ProductBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public ProductBuilder withOrders(Order... orders) {
            this.orders = Arrays.asList(orders);
            return this;
        }

        public Product build() {
            Product product = new Product(description);
            product.setId(id);
            product.setOrders(orders);
            return product;
        }
    }

    // Product DTO Builder
    public static class ProductDTOBuilder {
        private Long id = 1L;
        private String description = "Test Product";
        private List<Long> orderIds = new ArrayList<>();

        public ProductDTOBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public ProductDTOBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public ProductDTOBuilder withOrderIds(Long... orderIds) {
            this.orderIds = Arrays.asList(orderIds);
            return this;
        }

        public ProductDTO build() {
            ProductDTO dto = new ProductDTO();
            dto.setId(id);
            dto.setDescription(description);
            dto.setOrderIds(orderIds);
            return dto;
        }
    }

    // Product Request Builder
    public static class ProductRequestBuilder {
        private String description = "Test Product";

        public ProductRequestBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public ProductRequest build() {
            ProductRequest request = new ProductRequest();
            request.setDescription(description);
            return request;
        }
    }

    // Helper DTO builders
    public static OrderCustomerDTOBuilder anOrderCustomerDTO() {
        return new OrderCustomerDTOBuilder();
    }

    public static class OrderCustomerDTOBuilder {
        private Long id = 1L;
        private String name = "John Doe";

        public OrderCustomerDTOBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public OrderCustomerDTOBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public OrderCustomerDTO build() {
            OrderCustomerDTO dto = new OrderCustomerDTO();
            dto.setId(id);
            dto.setName(name);
            return dto;
        }
    }

    public static OrderProductDTOBuilder anOrderProductDTO() {
        return new OrderProductDTOBuilder();
    }

    public static class OrderProductDTOBuilder {
        private Long id = 1L;
        private String description = "Test Product";

        public OrderProductDTOBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public OrderProductDTOBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public OrderProductDTO build() {
            OrderProductDTO dto = new OrderProductDTO();
            dto.setId(id);
            dto.setDescription(description);
            return dto;
        }
    }

    public static CustomerOrderDTOBuilder aCustomerOrderDTO() {
        return new CustomerOrderDTOBuilder();
    }

    public static class CustomerOrderDTOBuilder {
        private Long id = 1L;
        private String description = "Test Order";

        public CustomerOrderDTOBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public CustomerOrderDTOBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public CustomerOrderDTO build() {
            CustomerOrderDTO dto = new CustomerOrderDTO();
            dto.setId(id);
            dto.setDescription(description);
            return dto;
        }
    }
}
