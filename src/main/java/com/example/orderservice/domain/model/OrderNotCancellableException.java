package com.example.orderservice.domain.model;

import java.util.UUID;

public class OrderNotCancellableException extends RuntimeException {

    public OrderNotCancellableException(UUID orderId, OrderStatus currentStatus) {
        super("Order %s in status %s cannot be cancelled".formatted(orderId, currentStatus));
    }
}