package com.securitease.store.service;

import com.securitease.store.dto.OrderCustomerDTO;
import com.securitease.store.dto.OrderDTO;
import com.securitease.store.dto.OrderRequest;
import com.securitease.store.entity.Customer;
import com.securitease.store.entity.Order;
import com.securitease.store.exception.ResourceNotFoundException;
import com.securitease.store.mapper.OrderMapper;
import com.securitease.store.repository.CustomerRepository;
import com.securitease.store.repository.OrderRepository;
import com.securitease.store.service.impl.OrderServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    CustomerRepository customerRepository;

    @Mock
    OrderMapper orderMapper;

    @Mock
    CacheService cacheService;

    @InjectMocks
    OrderServiceImpl service;

    private Customer customer1;
    private Customer customer2;
    private Order order1;
    private Order order2;

    private OrderDTO orderDTO1;
    private OrderDTO orderDTO2;

    private OrderRequest createReq;
    private OrderRequest updateReqSameCustomer;
    private OrderRequest updateReqDifferentCustomer;

    @BeforeEach
    void setUp() {
        customer1 = new Customer();
        customer1.setId(10L);
        customer1.setName("Alice");

        customer2 = new Customer();
        customer2.setId(20L);
        customer2.setName("Bob");

        order1 = new Order();
        order1.setId(1L);
        order1.setDescription("o1");
        order1.setCustomer(customer1);

        order2 = new Order();
        order2.setId(2L);
        order2.setDescription("o2");
        order2.setCustomer(customer1);

        orderDTO1 = dtoFrom(order1);
        orderDTO2 = dtoFrom(order2);

        createReq = new OrderRequest();
        createReq.setDescription("new");
        createReq.setCustomerId(10L);

        updateReqSameCustomer = new OrderRequest();
        updateReqSameCustomer.setDescription("upd");
        updateReqSameCustomer.setCustomerId(10L);

        updateReqDifferentCustomer = new OrderRequest();
        updateReqDifferentCustomer.setDescription("upd2");
        updateReqDifferentCustomer.setCustomerId(20L);
    }

    private static OrderDTO dtoFrom(Order o) {
        OrderDTO dto = new OrderDTO();
        dto.setId(o.getId());
        dto.setDescription(o.getDescription());
        OrderCustomerDTO c = new OrderCustomerDTO();
        c.setId(o.getCustomer().getId());
        c.setName(o.getCustomer().getName());
        dto.setCustomer(c);
        dto.setProducts(List.of()); // not exercising products here
        return dto;
    }

    // ------------------- getAllOrders -------------------

    @Test
    @DisplayName("getAllOrders returns mapped list with nested customer")
    void getAllOrders_ok() {
        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));
        when(orderMapper.ordersToOrderDTOs(List.of(order1, order2))).thenReturn(List.of(orderDTO1, orderDTO2));

        var result = service.getAllOrders();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getCustomer().getId()).isEqualTo(10L);
        verify(orderRepository).findAll();
        verify(orderMapper).ordersToOrderDTOs(List.of(order1, order2));
    }

    // ------------------- getOrders (paged) -------------------

    @Test
    @DisplayName("getOrders returns mapped page with nested customer")
    void getOrders_ok() {
        Pageable pageable = PageRequest.of(0, 2, Sort.by("id"));
        Page<Order> page = new PageImpl<>(List.of(order1), pageable, 1);

        when(orderRepository.findAll(pageable)).thenReturn(page);
        when(orderMapper.orderToOrderDTO(order1)).thenReturn(orderDTO1);

        var result = service.getOrders(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCustomer().getName()).isEqualTo("Alice");
        verify(orderRepository).findAll(pageable);
        verify(orderMapper).orderToOrderDTO(order1);
    }

    @Test
    @DisplayName("getOrderById returns Optional DTO when present")
    void getOrderById_found() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order1));
        when(orderMapper.orderToOrderDTO(order1)).thenReturn(orderDTO1);

        var res = service.getOrderById(1L);

        assertThat(res).isPresent();
        assertThat(res.get().getCustomer().getId()).isEqualTo(10L);
        verify(orderRepository).findById(1L);
        verify(orderMapper).orderToOrderDTO(order1);
    }

    @Test
    @DisplayName("getOrderById returns empty Optional when not found")
    void getOrderById_notFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        var res = service.getOrderById(99L);

        assertThat(res).isEmpty();
        verify(orderRepository).findById(99L);
        verifyNoInteractions(orderMapper);
    }

    @Test
    @DisplayName("getOrdersByCustomerId returns mapped list")
    void getOrdersByCustomerId_ok() {
        when(orderRepository.findByCustomerId(10L)).thenReturn(List.of(order1, order2));
        when(orderMapper.ordersToOrderDTOs(List.of(order1, order2))).thenReturn(List.of(orderDTO1, orderDTO2));

        var res = service.getOrdersByCustomerId(10L);

        assertThat(res).extracting(d -> d.getCustomer().getId()).containsExactly(10L, 10L);
        verify(orderRepository).findByCustomerId(10L);
        verify(orderMapper).ordersToOrderDTOs(List.of(order1, order2));
    }

    // ------------------- createOrder -------------------

    @Test
    @DisplayName("createOrder throws when customer not found")
    void createOrder_customerMissing_throws() {
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrder(createReq))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer");

        verify(customerRepository).findById(10L);
        verifyNoInteractions(orderRepository, orderMapper);
    }

    @Test
    @DisplayName("createOrder saves and maps nested customer")
    void createOrder_ok() {
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer1));

        Order saved = new Order();
        saved.setId(123L);
        saved.setDescription("new");
        saved.setCustomer(customer1);

        when(orderRepository.save(any(Order.class))).thenReturn(saved);
        when(orderMapper.orderToOrderDTO(saved)).thenReturn(dtoFrom(saved));

        var res = service.createOrder(createReq);

        assertThat(res.getId()).isEqualTo(123L);
        assertThat(res.getCustomer().getId()).isEqualTo(10L);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("new");
        assertThat(captor.getValue().getCustomer()).isEqualTo(customer1);
    }

    // ------------------- updateOrder -------------------

    @Test
    @DisplayName("updateOrder throws when order not found")
    void updateOrder_orderMissing_throws() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateOrder(1L, updateReqSameCustomer))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order");

        verify(orderRepository).findById(1L);
        verifyNoMoreInteractions(orderRepository);
        verifyNoInteractions(customerRepository, orderMapper);
    }

    @Test
    @DisplayName("updateOrder throws when new customer not found")
    void updateOrder_customerMissing_throws() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order1));
        when(customerRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateOrder(1L, updateReqSameCustomer))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer");

        verify(orderRepository).findById(1L);
        verify(customerRepository).findById(10L);
        verifyNoInteractions(orderMapper);
    }

    @Test
    @DisplayName("updateOrder updates fields when customer unchanged")
    void updateOrder_sameCustomer_ok() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order1)); // old customer: 10
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer1));

        Order saved = new Order();
        saved.setId(1L);
        saved.setDescription("upd");
        saved.setCustomer(customer1);

        when(orderRepository.save(any(Order.class))).thenReturn(saved);
        when(orderMapper.orderToOrderDTO(saved)).thenReturn(dtoFrom(saved));

        var res = service.updateOrder(1L, updateReqSameCustomer);

        assertThat(res.getDescription()).isEqualTo("upd");
        assertThat(res.getCustomer().getId()).isEqualTo(10L);
        verify(orderRepository).save(any(Order.class));
        verify(orderMapper).orderToOrderDTO(saved);
    }

    @Test
    @DisplayName("updateOrder updates and maps when customer changed")
    void updateOrder_customerChanged_ok() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order1)); // old customer: 10
        when(customerRepository.findById(20L)).thenReturn(Optional.of(customer2)); // new customer: 20

        Order saved = new Order();
        saved.setId(1L);
        saved.setDescription("upd2");
        saved.setCustomer(customer2);

        when(orderRepository.save(any(Order.class))).thenReturn(saved);
        when(orderMapper.orderToOrderDTO(saved)).thenReturn(dtoFrom(saved));

        var res = service.updateOrder(1L, updateReqDifferentCustomer);

        assertThat(res.getCustomer().getId()).isEqualTo(20L);
        verify(orderRepository).save(any(Order.class));
        verify(orderMapper).orderToOrderDTO(saved);
        // cache eviction for old/new customers is via annotations; method itself only logs
    }

    // ------------------- deleteOrder -------------------

    @Test
    @DisplayName("deleteOrder throws when not found")
    void deleteOrder_missing_throws() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteOrder(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order");

        verify(orderRepository).findById(99L);
        verify(orderRepository, never()).deleteById(anyLong());
        verifyNoInteractions(cacheService);
    }

    @Test
    @DisplayName("deleteOrder deletes and evicts customer orders cache (nested customer id)")
    void deleteOrder_ok_evicts() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order1)); // order1 -> customer1 (10)

        service.deleteOrder(1L);

        verify(orderRepository).deleteById(1L);
        verify(cacheService).evictCustomerOrders(10L);
    }
}
