package com.flashsale.inventory;

import com.flashsale.common.SaleStatus;
import com.flashsale.common.api.SaleResponse;
import com.flashsale.inventory.domain.SaleStock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

final class TestSaleFixtures {

    private TestSaleFixtures() {
    }

    static SaleResponse activeSale(UUID saleId, int initialStock) {
        return new SaleResponse(
                saleId,
                UUID.randomUUID(),
                "Sneakers",
                "Drop",
                BigDecimal.TEN,
                initialStock,
                Instant.now().minusSeconds(3600),
                Instant.now().plusSeconds(3600),
                SaleStatus.ACTIVE
        );
    }

    static SaleStock saleStock(UUID saleId, int initialStock) {
        SaleStock stock = new SaleStock();
        stock.setSaleId(saleId);
        stock.setInitialStock(initialStock);
        stock.setSold(0);
        return stock;
    }
}
