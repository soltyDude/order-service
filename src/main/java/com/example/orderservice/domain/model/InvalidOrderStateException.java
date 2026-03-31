package com.example.orderservice.domain.model;

import java.util.UUID;

public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(UUID orderId, OrderStatus currentStatus, OrderStatus targetStatus) {
        super("Order %s cannot transition from %s to %s".formatted(orderId, currentStatus, targetStatus));
    }
}