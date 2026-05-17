package com.flashsale.common.api;

import java.math.BigDecimal;
import java.util.UUID;

public record ChargeRequest(UUID orderId, BigDecimal amount) {
}
