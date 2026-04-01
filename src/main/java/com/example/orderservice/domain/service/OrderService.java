package com.example.orderservice.domain.service;

import com.example.orderservice.api.dto.request.CancelOrderRequest;
import com.example.orderservice.api.dto.request.CreateOrderRequest;
import com.example.orderservice.api.dto.response.OrderCancelDto;
import com.example.orderservice.api.dto.response.OrderDto;
import com.example.orderservice.domain.model.Order;
import com.example.orderservice.domain.model.OrderAccessDeniedException;
import com.example.orderservice.domain.model.OrderNotFoundException;
import com.example.orderservice.domain.model.OrderStatus;
import com.example.orderservice.infrastructure.persistence.entity.OrderReadModelEntity;
import com.example.orderservice.infrastructure.persistence.entity.OrderStatusHistoryEntity;
import com.example.orderservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.example.orderservice.infrastructure.persistence.repository.OrderReadModelRepository;
import com.example.orderservice.infrastructure.persistence.repository.OrderRepository;
import com.example.orderservice.infrastructure.persistence.repository.OrderStatusHistoryRepository;
import com.example.orderservice.infrastructure.persistence.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderReadModelRepository readModelRepository;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderDto createOrder(CreateOrderRequest request) {
        // 1. Build domain entity
        var shippingAddress = orderMapper.toShippingAddress(request.shippingAddress());
        var itemsData = orderMapper.toOrderItemDataList(request);

        var order = Order.create(
                request.userId(),
                shippingAddress,
                request.paymentMethod(),
                itemsData
        );

        // 2. Save order (CASCADE saves items too)
        order = orderRepository.save(order);
        log.info("Order created: orderId={}, userId={}, totalAmount={}",
                order.getId(), order.getUserId(), order.getTotalAmount());

        // 3. Record status history
        statusHistoryRepository.save(
                OrderStatusHistoryEntity.of(order.getId(), OrderStatus.PENDING.name())
        );

        // 4. Write outbox event (same transaction!)
        var outboxEvent = buildOutboxEvent(
                order.getId(),
                "order.created",
                buildOrderCreatedPayload(order)
        );
        outboxEventRepository.save(outboxEvent);

        // 5. Update read model (sync in Phase 1)
        var readModel = orderMapper.toReadModelEntity(order);
        readModelRepository.save(readModel);

        // 6. Return DTO
        return orderMapper.toOrderDto(readModel);
    }

    @Transactional
    public OrderCancelDto cancelOrder(UUID orderId, CancelOrderRequest request, UUID userId) {
        // 1. Find order
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 2. Check ownership
        if (!order.getUserId().equals(userId)) {
            throw new OrderAccessDeniedException(orderId, userId);
        }

        // 3. Cancel (state machine validates, throws if not cancellable)
        var cancelResult = order.cancel(request.reason());
        log.info("Order cancelled: orderId={}, reason={}, refundNeeded={}, releaseNeeded={}",
                orderId, request.reason(), cancelResult.refundNeeded(), cancelResult.releaseNeeded());

        // 4. Record status history
        statusHistoryRepository.save(
                OrderStatusHistoryEntity.of(orderId, OrderStatus.CANCELLED.name())
        );

        // 5. Save order (status changed to CANCELLED)
        orderRepository.save(order);

        // 6. Write outbox event for saga compensations
        var outboxEvent = buildOutboxEvent(
                orderId,
                "order.cancelled",
                buildOrderCancelledPayload(order, cancelResult)
        );
        outboxEventRepository.save(outboxEvent);

        // 7. Update read model
        var readModel = readModelRepository.findById(orderId)
                .orElseGet(() -> orderMapper.toReadModelEntity(order));
        readModel.setStatus(OrderStatus.CANCELLED.name());
        readModel.setCancelReason(request.reason());
        readModel.setUpdatedAt(order.getUpdatedAt());
        readModelRepository.save(readModel);

        // 8. Return DTO with compensations
        return orderMapper.toOrderCancelDto(order, cancelResult);
    }

    // ==================== Outbox helpers ====================

    private OutboxEventEntity buildOutboxEvent(UUID orderId, String eventType, Map<String, Object> payload) {
        return OutboxEventEntity.builder()
                .aggregateType("Order")
                .aggregateId(orderId.toString())
                .eventType(eventType)
                .payload(writeJson(payload))
                .build();
    }

    private Map<String, Object> buildOrderCreatedPayload(Order order) {
        return Map.of(
                "eventId", UUID.randomUUID().toString(),
                "timestamp", order.getCreatedAt().toString(),
                "orderId", order.getId().toString(),
                "userId", order.getUserId().toString(),
                "totalAmount", order.getTotalAmount().toString(),
                "currency", "USD",
                "paymentMethod", order.getPaymentMethod()
        );
    }

    private Map<String, Object> buildOrderCancelledPayload(Order order, Order.CancelResult cancelResult) {
        return Map.of(
                "eventId", UUID.randomUUID().toString(),
                "timestamp", order.getUpdatedAt().toString(),
                "orderId", order.getId().toString(),
                "reason", order.getCancelReason(),
                "refundNeeded", cancelResult.refundNeeded(),
                "releaseNeeded", cancelResult.releaseNeeded(),
                "paymentId", order.getPaymentId() != null ? order.getPaymentId() : "",
                "reservationId", order.getReservationId() != null ? order.getReservationId() : ""
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}