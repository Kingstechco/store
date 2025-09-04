package com.securitease.store.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "orders", // ✅ avoid reserved keyword
        indexes = {
            @Index(name = "idx_order_customer_id", columnList = "customer_id"),
            @Index(name = "idx_order_description", columnList = "description")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // convenient for test data or factory-style creation
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 5, max = 255)
    @Column(nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Builder.Default // ensures list is initialized even when using builder
    @ManyToMany(
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "order_product",
            joinColumns = @JoinColumn(name = "order_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id"))
    private List<Product> products = new ArrayList<>();

    public Order(String description, Customer customer) {
        this.description = description;
        this.customer = customer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Order{" + "id="
                + id + ", description='"
                + description + '\'' + ", customerId="
                + (customer != null ? customer.getId() : null) + '}';
    }
}
