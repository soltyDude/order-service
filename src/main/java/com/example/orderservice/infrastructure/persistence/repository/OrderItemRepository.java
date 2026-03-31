package com.example.orderservice.infrastructure.persistence.repository;

import com.example.orderservice.domain.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
}