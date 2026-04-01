package com.example.orderservice.domain.service;

import com.example.orderservice.api.dto.request.CancelOrderRequest;
import com.example.orderservice.api.dto.response.OrderCancelDto;
import com.example.orderservice.api.dto.response.OrderDto;
import com.example.orderservice.api.dto.response.OrderStatusDto;
import com.example.orderservice.api.dto.response.OrderSummaryDto;
import com.example.orderservice.domain.model.OrderAccessDeniedException;
import com.example.orderservice.domain.model.OrderNotFoundException;
import com.example.orderservice.infrastructure.persistence.repository.OrderReadModelRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderReadModelRepository readModelRepository;
    private final OrderMapper orderMapper;

    public OrderDto getOrder(UUID orderId, UUID userId) {
        var readModel = readModelRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!readModel.getUserId().equals(userId)) {
            throw new OrderAccessDeniedException(orderId, userId);
        }

        return orderMapper.toOrderDto(readModel);
    }

    public Page<OrderSummaryDto> getOrders(UUID userId, String status, Pageable pageable) {
        return readModelRepository.findByUserIdAndOptionalStatus(userId, status, pageable)
                .map(orderMapper::toOrderSummaryDto);
    }

    public OrderStatusDto getOrderStatus(UUID orderId, UUID userId) {
        var readModel = readModelRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!readModel.getUserId().equals(userId)) {
            throw new OrderAccessDeniedException(orderId, userId);
        }

        return orderMapper.toOrderStatusDto(readModel);
    }
}