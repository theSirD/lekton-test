package com.flashsale.inventory.web;

import com.flashsale.common.OrderStatus;
import com.flashsale.inventory.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID saleId,
        int quantity,
        OrderStatus status,
        BigDecimal amount,
        String userEmail,
        Instant createdAt
) {
    static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getSaleId(),
                order.getQuantity(),
                order.getStatus(),
                order.getAmount(),
                order.getUserEmail(),
                order.getCreatedAt()
        );
    }
}
