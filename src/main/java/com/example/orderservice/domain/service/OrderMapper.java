package com.example.orderservice.domain.service;

import com.example.orderservice.api.dto.response.OrderCancelDto;
import com.example.orderservice.api.dto.response.OrderDto;
import com.example.orderservice.api.dto.response.OrderItemDto;
import com.example.orderservice.api.dto.response.OrderStatusDto;
import com.example.orderservice.api.dto.response.OrderSummaryDto;
import com.example.orderservice.api.dto.response.CompensationsDto;
import com.example.orderservice.api.dto.response.ShippingAddressDto;
import com.example.orderservice.api.dto.request.CreateOrderRequest;
import com.example.orderservice.api.dto.request.ShippingAddressRequest;
import com.example.orderservice.domain.model.Order;
import com.example.orderservice.domain.model.ShippingAddress;
import com.example.orderservice.infrastructure.persistence.entity.OrderReadModelEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final ObjectMapper objectMapper;

    // ==================== Request → Domain ====================

    public ShippingAddress toShippingAddress(ShippingAddressRequest request) {
        return ShippingAddress.builder()
                .street(request.street())
                .city(request.city())
                .zipCode(request.zipCode())
                .country(request.country())
                .build();
    }

    public List<Order.OrderItemData> toOrderItemDataList(CreateOrderRequest request) {
        return request.items().stream()
                .map(item -> new Order.OrderItemData(
                        item.productId(),
                        item.quantity(),
                        item.price()))
                .toList();
    }

    // ==================== ReadModel → Response DTO ====================

    public OrderDto toOrderDto(OrderReadModelEntity readModel) {
        return new OrderDto(
                readModel.getOrderId(),
                readModel.getUserId(),
                readModel.getStatus(),
                readModel.getTotalAmount(),
                readModel.getItemCount(),
                deserializeItems(readModel.getItems()),
                readModel.getPaymentId(),
                readModel.getPaymentStatus(),
                readModel.getReservationId(),
                readModel.getPaymentMethod(),
                deserializeAddress(readModel.getShippingAddress()),
                readModel.getCancelReason(),
                readModel.getCreatedAt(),
                readModel.getUpdatedAt()
        );
    }

    public OrderSummaryDto toOrderSummaryDto(OrderReadModelEntity readModel) {
        return new OrderSummaryDto(
                readModel.getOrderId(),
                readModel.getStatus(),
                readModel.getTotalAmount(),
                readModel.getItemCount(),
                readModel.getPaymentMethod(),
                readModel.getCreatedAt()
        );
    }

    public OrderStatusDto toOrderStatusDto(OrderReadModelEntity readModel) {
        return new OrderStatusDto(
                readModel.getOrderId(),
                readModel.getStatus(),
                readModel.getPaymentStatus(),
                readModel.getReservationId()
        );
    }

    public OrderCancelDto toOrderCancelDto(Order order, Order.CancelResult cancelResult) {
        return new OrderCancelDto(
                order.getId(),
                order.getStatus().name(),
                order.getCancelReason(),
                new CompensationsDto(
                        cancelResult.refundNeeded(),
                        cancelResult.releaseNeeded()
                )
        );
    }

    // ==================== Entity → ReadModel ====================

    public OrderReadModelEntity toReadModelEntity(Order order) {
        return OrderReadModelEntity.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .itemCount(order.getItems().size())
                .items(serializeItems(order))
                .paymentId(order.getPaymentId())
                .paymentStatus(null)
                .reservationId(order.getReservationId())
                .paymentMethod(order.getPaymentMethod())
                .shippingAddress(serializeAddress(order.getShippingAddress()))
                .cancelReason(order.getCancelReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    // ==================== JSON serialization helpers ====================

    private String serializeItems(Order order) {
        var itemDtos = order.getItems().stream()
                .map(item -> new OrderItemDto(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getSubtotal()))
                .toList();
        return writeJson(itemDtos);
    }

    private String serializeAddress(ShippingAddress address) {
        return writeJson(new ShippingAddressDto(
                address.getStreet(),
                address.getCity(),
                address.getZipCode(),
                address.getCountry()
        ));
    }

    private List<OrderItemDto> deserializeItems(String json) {
        return readJson(json, new TypeReference<>() {});
    }

    private ShippingAddressDto deserializeAddress(String json) {
        return readJson(json, new TypeReference<>() {});
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize to JSON", e);
        }
    }

    private <T> T readJson(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize from JSON", e);
        }
    }
}