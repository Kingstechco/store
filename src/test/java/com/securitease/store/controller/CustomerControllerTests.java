package com.securitease.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securitease.store.dto.CustomerDTO;
import com.securitease.store.dto.CustomerRequest;
import com.securitease.store.exception.ResourceNotFoundException;
import com.securitease.store.service.CustomerService;
import com.securitease.store.testutil.TestDataBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CustomerController.class)
@WithMockUser
@DisplayName("CustomerController Unit Tests")
class CustomerControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    private CustomerDTO customerDTO;

    private CustomerRequest customerRequest;

    @BeforeEach
    void setUp() {
        // Use test data builder for consistent test data
        customerDTO =
                TestDataBuilder.aCustomerDTO().withId(1L).withName("John Doe").build();

        customerRequest =
                TestDataBuilder.aCustomerRequest().withName("John Doe").build();
    }

    @Nested
    @DisplayName("POST /api/v1/customers - Create Customer")
    class CreateCustomerTests {

        @Test
        @DisplayName("Should create customer successfully with valid request")
        void shouldCreateCustomerSuccessfully() throws Exception {
            // Given
            when(customerService.createCustomer(any(CustomerRequest.class))).thenReturn(customerDTO);

            // When & Then
            mockMvc.perform(post("/api/v1/customers")
                            .with(csrf()) // IMPORTANT if Spring Security is enabled
                            .contentType(APPLICATION_JSON)
                            .accept(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(customerRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("John Doe"));

            verify(customerService, times(1)).createCustomer(any(CustomerRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when name is blank")
        void shouldReturn400WhenNameIsBlank() throws Exception {
            // Given
            CustomerRequest invalidRequest =
                    TestDataBuilder.aCustomerRequest().withName("").build();

            // When & Then
            mockMvc.perform(post("/api/v1/customers")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.name").exists());

            verify(customerService, never()).createCustomer(any());
        }

        @Test
        @DisplayName("Should return 400 when name is too short")
        void shouldReturn400WhenNameIsTooShort() throws Exception {
            // Given
            CustomerRequest invalidRequest =
                    TestDataBuilder.aCustomerRequest().withName("X").build();

            // When & Then
            mockMvc.perform(post("/api/v1/customers")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.name").exists());

            verify(customerService, never()).createCustomer(any());
        }

        @Test
        @DisplayName("Should return 400 when request body is missing")
        void shouldReturn400WhenRequestBodyMissing() throws Exception {
            mockMvc.perform(post("/api/v1/customers")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());

            verify(customerService, never()).createCustomer(any());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/customers - Get All Customers")
    class GetAllCustomersTests {

        @Test
        @DisplayName("Should return all customers when no search parameter provided")
        void shouldReturnAllCustomersWhenNoSearchParameter() throws Exception {
            // Given
            List<CustomerDTO> customers = List.of(
                    TestDataBuilder.aCustomerDTO()
                            .withId(1L)
                            .withName("Musa Maringa")
                            .build(),
                    TestDataBuilder.aCustomerDTO()
                            .withId(2L)
                            .withName("Jane Smith")
                            .build());
            when(customerService.getAllCustomers()).thenReturn(customers);

            // When & Then
            mockMvc.perform(get("/api/v1/customers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name").value("Musa Maringa"))
                    .andExpect(jsonPath("$[1].name").value("Jane Smith"));

            verify(customerService, times(1)).getAllCustomers();
            verify(customerService, never()).findCustomersByNameContaining(anyString());
        }

        @Test
        @DisplayName("Should search customers when name parameter provided")
        void shouldSearchCustomersWhenNameParameterProvided() throws Exception {
            // Given
            List<CustomerDTO> customers = List.of(customerDTO);
            when(customerService.findCustomersByNameContaining("John")).thenReturn(customers);

            // When & Then
            mockMvc.perform(get("/api/v1/customers").param("name", "John"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("John Doe"));

            verify(customerService, times(1)).findCustomersByNameContaining("John");
            verify(customerService, never()).getAllCustomers();
        }

        @Test
        @DisplayName("Should return empty list when no customers found")
        void shouldReturnEmptyListWhenNoCustomersFound() throws Exception {
            // Given
            when(customerService.getAllCustomers()).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/v1/customers")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

            verify(customerService, times(1)).getAllCustomers();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/customers/paged - Get Customers with Pagination")
    class GetCustomersPagedTests {

        @Test
        @DisplayName("Should return paginated customers")
        void shouldReturnPaginatedCustomers() throws Exception {
            // Given
            List<CustomerDTO> customers = List.of(customerDTO);
            Page<CustomerDTO> page = new PageImpl<>(customers, PageRequest.of(0, 20), 1);
            when(customerService.getCustomers(any())).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/v1/customers/paged").param("page", "0").param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("John Doe"));

            verify(customerService, times(1)).getCustomers(any());
        }

        @Test
        @DisplayName("Should cap page size to maximum allowed by Spring Boot (100)")
        void shouldCapPageSizeToMaximumAllowed() throws Exception {
            // Capture the pageable that gets passed to the service
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            
            // Create mock page with capped size
            when(customerService.getCustomers(pageableCaptor.capture())).thenAnswer(invocation -> {
                Pageable pageable = pageableCaptor.getValue();
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            });

            mockMvc.perform(get("/api/v1/customers/paged").param("page", "0").param("size", "101"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size").value(100)); // Spring Boot caps to 100

            // Verify that service was called with capped size
            assertEquals(100, pageableCaptor.getValue().getPageSize());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/customers/{id} - Get Customer by ID")
    class GetCustomerByIdTests {

        @Test
        @DisplayName("Should return customer when found")
        void shouldReturnCustomerWhenFound() throws Exception {
            // Given
            when(customerService.getCustomerById(1L)).thenReturn(Optional.of(customerDTO));

            // When & Then
            mockMvc.perform(get("/api/v1/customers/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("John Doe"));

            verify(customerService, times(1)).getCustomerById(1L);
        }

        @Test
        @DisplayName("Should return 404 when customer not found")
        void shouldReturn404WhenCustomerNotFound() throws Exception {
            // Given
            when(customerService.getCustomerById(999L)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/v1/customers/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

            verify(customerService, times(1)).getCustomerById(999L);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/customers/{id} - Update Customer")
    class UpdateCustomerTests {

        @Test
        @DisplayName("Should update customer successfully")
        void shouldUpdateCustomerSuccessfully() throws Exception {
            // Given
            CustomerDTO updatedCustomer = TestDataBuilder.aCustomerDTO()
                    .withId(1L)
                    .withName("John Updated")
                    .build();
            CustomerRequest updateRequest =
                    TestDataBuilder.aCustomerRequest().withName("John Updated").build();

            when(customerService.updateCustomer(eq(1L), any(CustomerRequest.class)))
                    .thenReturn(updatedCustomer);

            // When & Then
            mockMvc.perform(put("/api/v1/customers/1")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("John Updated"));

            verify(customerService, times(1)).updateCustomer(eq(1L), any());
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent customer")
        void shouldReturn404WhenUpdatingNonExistentCustomer() throws Exception {
            // Given
            when(customerService.updateCustomer(eq(999L), any()))
                    .thenThrow(new ResourceNotFoundException("Customer", "id", 999L));

            // When & Then
            mockMvc.perform(put("/api/v1/customers/999")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(customerRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

            verify(customerService, times(1)).updateCustomer(eq(999L), any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/customers/{id} - Delete Customer")
    class DeleteCustomerTests {

        @Test
        @DisplayName("Should delete customer successfully")
        void shouldDeleteCustomerSuccessfully() throws Exception {
            // Given
            doNothing().when(customerService).deleteCustomer(1L);

            // When & Then
            mockMvc.perform(delete("/api/v1/customers/1").with(csrf())).andExpect(status().isNoContent());

            verify(customerService, times(1)).deleteCustomer(1L);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent customer")
        void shouldReturn404WhenDeletingNonExistentCustomer() throws Exception {
            // Given
            doThrow(new ResourceNotFoundException("Customer", "id", 999L))
                    .when(customerService)
                    .deleteCustomer(999L);

            // When & Then
            mockMvc.perform(delete("/api/v1/customers/999").with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

            verify(customerService, times(1)).deleteCustomer(999L);
        }
    }
}
