package com.flashsale.common.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OrderConfirmedEvent.class, name = "OrderConfirmed"),
        @JsonSubTypes.Type(value = OrderCancelledEvent.class, name = "OrderCancelled")
})
public sealed interface OrderEvent permits OrderConfirmedEvent, OrderCancelledEvent {

    String eventType();

    UUID orderId();

    String userEmail();

    UUID saleId();

    int quantity();

    BigDecimal amount();

    Instant occurredAt();
}
