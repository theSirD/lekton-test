package com.flashsale.common.api;

import com.flashsale.common.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaleResponse(
        UUID id,
        UUID productId,
        String productName,
        String productDescription,
        BigDecimal price,
        int initialStock,
        Instant startsAt,
        Instant endsAt,
        SaleStatus status
) {
}
