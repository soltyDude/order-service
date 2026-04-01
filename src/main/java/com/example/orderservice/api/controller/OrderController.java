package com.example.orderservice.api.controller;

import com.example.orderservice.api.dto.request.CancelOrderRequest;
import com.example.orderservice.api.dto.request.CreateOrderRequest;
import com.example.orderservice.api.dto.response.OrderCancelDto;
import com.example.orderservice.api.dto.response.OrderDto;
import com.example.orderservice.api.dto.response.OrderStatusDto;
import com.example.orderservice.api.dto.response.OrderSummaryDto;
import com.example.orderservice.domain.service.OrderQueryService;
import com.example.orderservice.domain.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        var order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{orderId}")
    public OrderDto getOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID userId) {
        return orderQueryService.getOrder(orderId, userId);
    }

    @GetMapping
    public Page<OrderSummaryDto> getOrders(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return orderQueryService.getOrders(userId, status, pageable);
    }

    @PatchMapping("/{orderId}/cancel")
    public OrderCancelDto cancelOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody CancelOrderRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return orderService.cancelOrder(orderId, request, userId);
    }

    @GetMapping("/{orderId}/status")
    public OrderStatusDto getOrderStatus(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID userId) {
        return orderQueryService.getOrderStatus(orderId, userId);
    }
}