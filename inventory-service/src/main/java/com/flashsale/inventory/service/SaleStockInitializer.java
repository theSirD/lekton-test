package com.flashsale.inventory.service;

import com.flashsale.common.api.SaleResponse;
import com.flashsale.inventory.domain.SaleStock;
import com.flashsale.inventory.redis.RedisStockService;
import com.flashsale.inventory.repository.SaleStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SaleStockInitializer {

    private final SaleStockRepository saleStockRepository;
    private final RedisStockService redisStockService;

    @Transactional
    public void ensureInitialized(SaleResponse sale) {
        SaleStock stock = saleStockRepository.findById(sale.id()).orElseGet(() -> {
            SaleStock created = new SaleStock();
            created.setSaleId(sale.id());
            created.setInitialStock(sale.initialStock());
            created.setSold(0);
            return saleStockRepository.save(created);
        });
        int available = stock.getInitialStock() - stock.getSold();
        redisStockService.initializeStock(sale.id(), available);
    }
}
