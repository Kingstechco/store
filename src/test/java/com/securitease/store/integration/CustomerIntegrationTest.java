package com.securitease.store.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securitease.store.dto.CustomerDTO;
import com.securitease.store.dto.CustomerRequest;
import com.securitease.store.testutil.TestDataBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Customer API endpoints.
 *
 * <p>These tests verify the complete end-to-end functionality of customer operations including database persistence,
 * transaction management, and HTTP layer integration. Uses TestContainers for isolated database testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = {CustomerIntegrationTest.Initializer.class})
@DirtiesContext
@DisplayName("Customer Integration Tests")
class CustomerIntegrationTest {

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

    @Nested
    @DisplayName("Customer CRUD Operations")
    class CustomerCrudTests {

        @Test
        @WithMockUser
        @DisplayName("Should create, retrieve, update and delete customer successfully")
        void shouldPerformFullCustomerCrudOperations() throws Exception {
            // 1. Create customer
            CustomerRequest createRequest = TestDataBuilder.aCustomerRequest()
                    .withName("Integration Test Customer")
                    .build();

            String createResponse = mockMvc.perform(post("/api/v1/customers")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.name").value("Integration Test Customer"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            CustomerDTO createdCustomer = objectMapper.readValue(createResponse, CustomerDTO.class);
            Long customerId = createdCustomer.getId();

            // 2. Retrieve customer
            mockMvc.perform(get("/api/v1/customers/{id}", customerId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(customerId))
                    .andExpect(jsonPath("$.name").value("Integration Test Customer"));

            // 3. Update customer
            CustomerRequest updateRequest = TestDataBuilder.aCustomerRequest()
                    .withName("Updated Integration Customer")
                    .build();

            mockMvc.perform(put("/api/v1/customers/{id}", customerId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Integration Customer"));

            // 4. Delete customer
            mockMvc.perform(delete("/api/v1/customers/{id}", customerId)
                            .with(csrf())).andExpect(status().isNoContent());

            // 5. Verify deletion
            mockMvc.perform(get("/api/v1/customers/{id}", customerId)).andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("Should search customers by name substring")
        void shouldSearchCustomersByName() throws Exception {
            // Create test customers
            CustomerRequest customer1 =
                    TestDataBuilder.aCustomerRequest().withName("John Doe").build();
            CustomerRequest customer2 =
                    TestDataBuilder.aCustomerRequest().withName("Jane Smith").build();
            CustomerRequest customer3 =
                    TestDataBuilder.aCustomerRequest().withName("John Williams").build();

            // Create customers
            mockMvc.perform(post("/api/v1/customers")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(customer1)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/v1/customers")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(customer2)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/v1/customers")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(customer3)))
                    .andExpect(status().isCreated());

            // Search for customers with "John" in name
            mockMvc.perform(get("/api/v1/customers").param("name", "John"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].name", containsInAnyOrder("John Doe", "John Williams")));
        }
    }

    @Nested
    @DisplayName("Customer Validation Tests")
    class CustomerValidationTests {

        @Test
        @WithMockUser
        @DisplayName("Should reject invalid customer creation requests")
        void shouldRejectInvalidCustomerRequests() throws Exception {
            // Test empty name
            CustomerRequest invalidRequest =
                    TestDataBuilder.aCustomerRequest().withName("").build();

            mockMvc.perform(post("/api/v1/customers")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.name").exists());

            // Test name too short
            CustomerRequest tooShortRequest =
                    TestDataBuilder.aCustomerRequest().withName("X").build();

            mockMvc.perform(post("/api/v1/customers")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tooShortRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.name").exists());
        }
    }

    @Nested
    @DisplayName("Customer Pagination Tests")
    class CustomerPaginationTests {

        @Test
        @WithMockUser
        @DisplayName("Should handle pagination correctly")
        void shouldHandlePaginationCorrectly() throws Exception {
            // Create multiple customers for pagination testing
            for (int i = 1; i <= 25; i++) {
                CustomerRequest customer = TestDataBuilder.aCustomerRequest()
                        .withName("Customer " + i)
                        .build();

                mockMvc.perform(post("/api/v1/customers")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(customer)))
                        .andExpect(status().isCreated());
            }

            // Test first page
            mockMvc.perform(get("/api/v1/customers/paged").param("page", "0").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(10)))
                    .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(25)))
                    .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(3)));

            // Test page size capping - Spring Boot caps at 100, returns 200
            mockMvc.perform(get("/api/v1/customers/paged").param("page", "0").param("size", "101"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size").value(100)); // Spring Boot caps to 100
        }
    }
}
