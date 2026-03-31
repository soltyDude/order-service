package com.example.orderservice.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_status_history")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant timestamp;

    public static OrderStatusHistoryEntity of(UUID orderId, String status) {
        return OrderStatusHistoryEntity.builder()
                .orderId(orderId)
                .status(status)
                .timestamp(Instant.now())
                .build();
    }
}