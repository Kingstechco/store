package com.securitease.store.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securitease.store.dto.*;
import com.securitease.store.testutil.TestDataBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Order-Customer workflow scenarios.
 *
 * <p>These tests verify complete business workflows involving multiple entities and their relationships, ensuring
 * proper transaction management and data consistency across the entire application stack.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = {OrderCustomerWorkflowIntegrationTest.Initializer.class})
@DirtiesContext
@DisplayName("Order-Customer Workflow Integration Tests")
class OrderCustomerWorkflowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.2")
            .withDatabaseName("store_test")
            .withUsername("test")
            .withPassword("test");

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                            "spring.datasource.url=" + postgres.getJdbcUrl(),
                            "spring.datasource.username=" + postgres.getUsername(),
                            "spring.datasource.password=" + postgres.getPassword(),
                            "spring.cache.type=simple",
                            "spring.jpa.hibernate.ddl-auto=create-drop")
                    .applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @org.junit.jupiter.api.Disabled("TODO: Fix customer-order relationship loading in integration test")
    @DisplayName("Should complete full customer-order-product workflow")
    void shouldCompleteFullWorkflow() throws Exception {
        // 1. Create customer
        CustomerRequest customerRequest = TestDataBuilder.aCustomerRequest()
                .withName("Workflow Test Customer")
                .build();

        String customerResponse = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CustomerDTO customer = objectMapper.readValue(customerResponse, CustomerDTO.class);
        Long customerId = customer.getId();

        // 2. Create products
        ProductRequest product1Request = TestDataBuilder.aProductRequest()
                .withDescription("Laptop Computer")
                .build();
        ProductRequest product2Request = TestDataBuilder.aProductRequest()
                .withDescription("Wireless Mouse")
                .build();

        String product1Response = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product1Request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String product2Response = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product2Request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ProductDTO product1 = objectMapper.readValue(product1Response, ProductDTO.class);
        ProductDTO product2 = objectMapper.readValue(product2Response, ProductDTO.class);

        // 3. Create order for customer
        OrderRequest orderRequest = TestDataBuilder.anOrderRequest()
                .withDescription("Customer's first order")
                .withCustomerId(customerId)
                .build();

        String orderResponse = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        OrderDTO order = objectMapper.readValue(orderResponse, OrderDTO.class);
        Long orderId = order.getId();

        // 4. Verify order contains customer information
        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.description").value("Customer's first order"))
                .andExpect(jsonPath("$.customer.id").value(customerId))
                .andExpect(jsonPath("$.customer.name").value("Workflow Test Customer"));

        // 5. Verify customer's orders
        mockMvc.perform(get("/api/v1/orders/customers/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(orderId))
                .andExpect(jsonPath("$[0].description").value("Customer's first order"));

        // 6. Verify products exist and are accessible
        mockMvc.perform(get("/api/v1/products/{id}", product1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Laptop Computer"));

        mockMvc.perform(get("/api/v1/products/{id}", product2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Wireless Mouse"));

        // 7. Verify customer can be retrieved with orders
        mockMvc.perform(get("/api/v1/customers/{id}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Workflow Test Customer"))
                .andExpect(jsonPath("$.orders", hasSize(1)))
                .andExpect(jsonPath("$.orders[0].description").value("Customer's first order"));
    }

    @Test
    @DisplayName("Should handle cascading deletes properly")
    void shouldHandleCascadingDeletesProperly() throws Exception {
        // Create customer
        CustomerRequest customerRequest = TestDataBuilder.aCustomerRequest()
                .withName("Delete Test Customer")
                .build();

        String customerResponse = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CustomerDTO customer = objectMapper.readValue(customerResponse, CustomerDTO.class);
        Long customerId = customer.getId();

        // Create order for customer
        OrderRequest orderRequest = TestDataBuilder.anOrderRequest()
                .withDescription("Order to be deleted")
                .withCustomerId(customerId)
                .build();

        String orderResponse = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        OrderDTO order = objectMapper.readValue(orderResponse, OrderDTO.class);
        Long orderId = order.getId();

        // Verify order exists
        mockMvc.perform(get("/api/v1/orders/{id}", orderId)).andExpect(status().isOk());

        // Delete customer (should handle cascading properly)
        mockMvc.perform(delete("/api/v1/customers/{id}", customerId)).andExpect(status().isNoContent());

        // Verify customer is deleted
        mockMvc.perform(get("/api/v1/customers/{id}", customerId)).andExpect(status().isNotFound());

        // Verify order still exists but customer reference is handled properly
        mockMvc.perform(get("/api/v1/orders/{id}", orderId)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle transaction rollback on validation errors")
    void shouldHandleTransactionRollback() throws Exception {
        // Create customer
        CustomerRequest customerRequest = TestDataBuilder.aCustomerRequest()
                .withName("Transaction Test Customer")
                .build();

        String customerResponse = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CustomerDTO customer = objectMapper.readValue(customerResponse, CustomerDTO.class);
        Long customerId = customer.getId();

        // Try to create order with invalid data (missing description)
        OrderRequest invalidOrderRequest = TestDataBuilder.anOrderRequest()
                .withDescription("") // Invalid - empty description
                .withCustomerId(customerId)
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidOrderRequest)))
                .andExpect(status().isBadRequest());

        // Verify customer still exists and no orders were created
        mockMvc.perform(get("/api/v1/customers/{id}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders", hasSize(0)));

        // Verify no orders exist for this customer
        mockMvc.perform(get("/api/v1/orders/customers/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
