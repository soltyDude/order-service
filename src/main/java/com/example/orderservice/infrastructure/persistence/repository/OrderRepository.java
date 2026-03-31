package com.example.orderservice.infrastructure.persistence.repository;

import com.example.orderservice.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}