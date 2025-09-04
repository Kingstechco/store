package com.example.store.controller;

import com.example.store.dto.OrderCustomerDTO;
import com.example.store.dto.OrderDTO;
import com.example.store.dto.OrderRequest;
import com.example.store.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private OrderDTO orderDTO;
    private OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        OrderCustomerDTO customerDTO = new OrderCustomerDTO();
        customerDTO.setName("John Doe");
        customerDTO.setId(1L);

        orderDTO = new OrderDTO();
        orderDTO.setDescription("Test Order");
        orderDTO.setId(1L);
        orderDTO.setCustomer(customerDTO);

        orderRequest = new OrderRequest();
        orderRequest.setDescription("Test Order");
        orderRequest.setCustomerId(1L);
    }

    @Test
    void testCreateOrder() throws Exception {
        when(orderService.createOrder(any(OrderRequest.class))).thenReturn(orderDTO);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Test Order"))
                .andExpect(jsonPath("$.customer.name").value("John Doe"));
    }

    @Test
    void testGetOrder() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of(orderDTO));

        mockMvc.perform(get("/order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Test Order"))
                .andExpect(jsonPath("$[0].customer.name").value("John Doe"));
    }
}
