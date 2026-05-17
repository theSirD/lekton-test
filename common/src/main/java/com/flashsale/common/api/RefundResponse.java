package com.flashsale.common.api;

import com.flashsale.common.PaymentStatus;

import java.util.UUID;

public record RefundResponse(UUID orderId, PaymentStatus status) {
}
