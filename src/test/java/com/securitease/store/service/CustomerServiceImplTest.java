package com.securitease.store.service;

import com.securitease.store.dto.CustomerDTO;
import com.securitease.store.dto.CustomerRequest;
import com.securitease.store.entity.Customer;
import com.securitease.store.exception.ResourceNotFoundException;
import com.securitease.store.mapper.CustomerMapper;
import com.securitease.store.repository.CustomerRepository;
import com.securitease.store.service.impl.CustomerServiceImpl;
import com.securitease.store.testutil.TestDataBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService Unit Tests")
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private Customer existingCustomer;
    private CustomerDTO existingCustomerDto;
    private CustomerRequest createRequest;

    @BeforeEach
    void setUp() {
        existingCustomer =
                TestDataBuilder.aCustomer().withId(1L).withName("Musa Marvin").build();

        existingCustomerDto = TestDataBuilder.aCustomerDTO()
                .withId(1L)
                .withName("Musa Marvin")
                .build();

        createRequest =
                TestDataBuilder.aCustomerRequest().withName("Musa Marvin").build();
    }

    @Nested
    @DisplayName("Get All Customers")
    class GetAllCustomersTests {

        @Test
        @DisplayName("Should return all customers successfully")
        void shouldReturnAllCustomersSuccessfully() {
            List<Customer> customerEntities = List.of(existingCustomer);
            List<CustomerDTO> customerDtos = List.of(existingCustomerDto);

            when(customerRepository.findAll()).thenReturn(customerEntities);
            when(customerMapper.customersToCustomerDTOs(customerEntities)).thenReturn(customerDtos);

            List<CustomerDTO> result = customerService.getAllCustomers();

            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Musa Marvin");

            verify(customerRepository, times(1)).findAll();
            verify(customerMapper, times(1)).customersToCustomerDTOs(customerEntities);
        }

        @Test
        @DisplayName("Should return empty list when no customers exist")
        void shouldReturnEmptyListWhenNoCustomersExist() {
            when(customerRepository.findAll()).thenReturn(List.of());
            when(customerMapper.customersToCustomerDTOs(List.of())).thenReturn(List.of());

            List<CustomerDTO> result = customerService.getAllCustomers();

            assertThat(result).isNotNull();
            assertThat(result).isEmpty();

            verify(customerRepository, times(1)).findAll();
            verify(customerMapper, times(1)).customersToCustomerDTOs(List.of());
        }
    }

    @Nested
    @DisplayName("Get Customers with Pagination")
    class GetCustomersPagedTests {

        @Test
        @DisplayName("Should return paginated customers successfully")
        void shouldReturnPaginatedCustomersSuccessfully() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Customer> pageOfCustomers = new PageImpl<>(List.of(existingCustomer), pageable, 1);

            when(customerRepository.findAll(pageable)).thenReturn(pageOfCustomers);
            when(customerMapper.customerToCustomerDTO(existingCustomer)).thenReturn(existingCustomerDto);

            Page<CustomerDTO> result = customerService.getCustomers(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Musa Marvin");

            verify(customerRepository, times(1)).findAll(pageable);
            verify(customerMapper, times(1)).customerToCustomerDTO(existingCustomer);
        }
    }

    @Nested
    @DisplayName("Get Customer by ID")
    class GetCustomerByIdTests {

        @Test
        @DisplayName("Should return customer when found")
        void shouldReturnCustomerWhenFound() {
            when(customerRepository.findWithOrdersById(1L)).thenReturn(Optional.of(existingCustomer));
            when(customerMapper.customerToCustomerDTO(existingCustomer)).thenReturn(existingCustomerDto);

            Optional<CustomerDTO> result = customerService.getCustomerById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Musa Marvin");

            verify(customerRepository, times(1)).findWithOrdersById(1L);
            verify(customerMapper, times(1)).customerToCustomerDTO(existingCustomer);
        }

        @Test
        @DisplayName("Should return empty when customer not found")
        void shouldReturnEmptyWhenCustomerNotFound() {
            when(customerRepository.findWithOrdersById(999L)).thenReturn(Optional.empty());

            Optional<CustomerDTO> result = customerService.getCustomerById(999L);

            assertThat(result).isEmpty();

            verify(customerRepository, times(1)).findWithOrdersById(999L);
            verify(customerMapper, never()).customerToCustomerDTO(any());
        }
    }

    @Nested
    @DisplayName("Find Customers by Name")
    class FindCustomersByNameTests {

        @Test
        @DisplayName("Should find customers by name containing text")
        void shouldFindCustomersByNameContainingText() {
            String searchTerm = "Musa";
            List<Customer> customerEntities = List.of(existingCustomer);
            List<CustomerDTO> customerDtos = List.of(existingCustomerDto);

            when(customerRepository.findByNameContainingIgnoreCaseWithOrders(searchTerm))
                    .thenReturn(customerEntities);
            when(customerMapper.customersToCustomerDTOs(customerEntities)).thenReturn(customerDtos);

            List<CustomerDTO> result = customerService.findCustomersByNameContaining(searchTerm);

            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Musa Marvin");

            verify(customerRepository, times(1)).findByNameContainingIgnoreCaseWithOrders(searchTerm);
            verify(customerMapper, times(1)).customersToCustomerDTOs(customerEntities);
        }

        @Test
        @DisplayName("Should return empty list when no customers match search")
        void shouldReturnEmptyListWhenNoCustomersMatchSearch() {
            String searchTerm = "NonExistent";

            when(customerRepository.findByNameContainingIgnoreCaseWithOrders(searchTerm))
                    .thenReturn(List.of());
            when(customerMapper.customersToCustomerDTOs(List.of())).thenReturn(List.of());

            List<CustomerDTO> result = customerService.findCustomersByNameContaining(searchTerm);

            assertThat(result).isNotNull();
            assertThat(result).isEmpty();

            verify(customerRepository, times(1)).findByNameContainingIgnoreCaseWithOrders(searchTerm);
            verify(customerMapper, times(1)).customersToCustomerDTOs(List.of());
        }
    }

    @Nested
    @DisplayName("Create Customer")
    class CreateCustomerTests {

        @Test
        @DisplayName("Should create customer successfully")
        void shouldCreateCustomerSuccessfully() {
            Customer savedCustomer = TestDataBuilder.aCustomer()
                    .withId(1L)
                    .withName("Musa Marvin")
                    .build();

            when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
            when(customerMapper.customerToCustomerDTO(savedCustomer)).thenReturn(existingCustomerDto);

            CustomerDTO result = customerService.createCustomer(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Musa Marvin");

            verify(customerRepository, times(1)).save(any(Customer.class));
            verify(customerMapper, times(1)).customerToCustomerDTO(savedCustomer);
        }

        @Test
        @DisplayName("Should surface repository save errors")
        void shouldSurfaceRepositorySaveErrors() {
            when(customerRepository.save(any(Customer.class))).thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> customerService.createCustomer(createRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");

            verify(customerRepository, times(1)).save(any(Customer.class));
            verify(customerMapper, never()).customerToCustomerDTO(any());
        }
    }

    @Nested
    @DisplayName("Update Customer")
    class UpdateCustomerTests {

        @Test
        @DisplayName("Should update customer successfully")
        void shouldUpdateCustomerSuccessfully() {
            CustomerRequest updateRequest =
                    TestDataBuilder.aCustomerRequest().withName("Musa Updated").build();

            Customer updatedCustomer = TestDataBuilder.aCustomer()
                    .withId(1L)
                    .withName("Musa Updated")
                    .build();

            CustomerDTO updatedCustomerDto = TestDataBuilder.aCustomerDTO()
                    .withId(1L)
                    .withName("Musa Updated")
                    .build();

            when(customerRepository.findById(1L)).thenReturn(Optional.of(existingCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(updatedCustomer);
            when(customerMapper.customerToCustomerDTO(updatedCustomer)).thenReturn(updatedCustomerDto);

            CustomerDTO result = customerService.updateCustomer(1L, updateRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Musa Updated");

            verify(customerRepository, times(1)).findById(1L);
            verify(customerRepository, times(1)).save(any(Customer.class));
            verify(customerMapper, times(1)).customerToCustomerDTO(updatedCustomer);
        }

        @Test
        @DisplayName("Should throw when customer not found")
        void shouldThrowWhenCustomerNotFound() {
            when(customerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.updateCustomer(999L, createRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Customer"); // avoid brittle exact message assertions

            verify(customerRepository, times(1)).findById(999L);
            verify(customerRepository, never()).save(any());
            verify(customerMapper, never()).customerToCustomerDTO(any());
        }
    }

    @Nested
    @DisplayName("Delete Customer")
    class DeleteCustomerTests {

        @Test
        @DisplayName("Should delete customer successfully")
        void shouldDeleteCustomerSuccessfully() {
            when(customerRepository.existsById(1L)).thenReturn(true);
            doNothing().when(customerRepository).deleteById(1L);

            customerService.deleteCustomer(1L);

            verify(customerRepository, times(1)).existsById(1L);
            verify(customerRepository, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw when customer not found")
        void shouldThrowWhenCustomerNotFound() {
            when(customerRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> customerService.deleteCustomer(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Customer");

            verify(customerRepository, times(1)).existsById(999L);
            verify(customerRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should surface repository delete errors")
        void shouldSurfaceRepositoryDeleteErrors() {
            when(customerRepository.existsById(1L)).thenReturn(true);
            doThrow(new RuntimeException("Database error"))
                    .when(customerRepository)
                    .deleteById(1L);

            assertThatThrownBy(() -> customerService.deleteCustomer(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");

            verify(customerRepository, times(1)).existsById(1L);
            verify(customerRepository, times(1)).deleteById(1L);
        }
    }
}
