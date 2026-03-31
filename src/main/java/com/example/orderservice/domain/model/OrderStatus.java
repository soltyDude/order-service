package com.example.orderservice.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {

    PENDING,
    PAYMENT_PROCESSING,
    PAYMENT_CONFIRMED,
    INVENTORY_RESERVING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        VALID_TRANSITIONS.put(PENDING, EnumSet.of(PAYMENT_PROCESSING, CANCELLED));
        VALID_TRANSITIONS.put(PAYMENT_PROCESSING, EnumSet.of(PAYMENT_CONFIRMED, CANCELLED));
        VALID_TRANSITIONS.put(PAYMENT_CONFIRMED, EnumSet.of(INVENTORY_RESERVING, CANCELLED));
        VALID_TRANSITIONS.put(INVENTORY_RESERVING, EnumSet.of(CONFIRMED, CANCELLED));
        VALID_TRANSITIONS.put(CONFIRMED, EnumSet.of(SHIPPED, CANCELLED));
        VALID_TRANSITIONS.put(SHIPPED, EnumSet.of(DELIVERED));
        // DELIVERED и CANCELLED — терминальные, переходов нет

        /*VALID_TRANSITIONS.put(DELIVERED, EnumSet.noneOf(OrderStatus.class));
          VALID_TRANSITIONS.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));*/
    }

    public boolean canTransitionTo(OrderStatus target) {
        if (target == null) {
            return false;
        }
        return VALID_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    /**
     * Набор статусов, из которых можно отменить заказ.
     * Используется в Order.cancel() для быстрой проверки.
     */
    private static final Set<OrderStatus> CANCELLABLE = EnumSet.of(
            PENDING, PAYMENT_PROCESSING, PAYMENT_CONFIRMED, INVENTORY_RESERVING, CONFIRMED
    );

    public boolean isCancellable() {
        return CANCELLABLE.contains(this);
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }
}