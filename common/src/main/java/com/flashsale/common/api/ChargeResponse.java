package com.flashsale.common.api;

import com.flashsale.common.PaymentStatus;

import java.util.UUID;

public record ChargeResponse(UUID paymentId, UUID orderId, PaymentStatus status) {
}
