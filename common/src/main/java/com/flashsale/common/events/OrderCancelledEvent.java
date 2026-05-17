package com.flashsale.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID orderId,
        String userEmail,
        UUID saleId,
        int quantity,
        BigDecimal amount,
        Instant occurredAt
) implements OrderEvent {

    @Override
    public String eventType() {
        return "OrderCancelled";
    }
}
