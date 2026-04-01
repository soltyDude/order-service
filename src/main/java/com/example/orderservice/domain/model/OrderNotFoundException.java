package com.example.orderservice.domain.model;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID orderId) {
        super("Order not found: %s".formatted(orderId));
    }
}