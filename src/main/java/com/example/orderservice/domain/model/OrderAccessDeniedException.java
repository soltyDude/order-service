package com.example.orderservice.domain.model;

import java.util.UUID;

public class OrderAccessDeniedException extends RuntimeException {

    public OrderAccessDeniedException(UUID orderId, UUID userId) {
        super("User %s does not have access to order %s".formatted(userId, orderId));
    }
}
