package com.flashsale.inventory;

import com.flashsale.common.OrderStatus;
import com.flashsale.common.PaymentStatus;
import com.flashsale.common.SaleStatus;
import com.flashsale.common.api.ChargeRequest;
import com.flashsale.common.api.ChargeResponse;
import com.flashsale.common.api.SaleResponse;
import com.flashsale.inventory.domain.SaleStock;
import com.flashsale.inventory.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ConcurrencyIntegrationTest extends AbstractIntegrationTest {

    private static final UUID SALE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        saleStockRepository.deleteAll();

        SaleResponse sale = new SaleResponse(
                SALE_ID,
                UUID.randomUUID(),
                "Sneakers",
                "Drop",
                BigDecimal.TEN,
                50,
                Instant.now().minusSeconds(3600),
                Instant.now().plusSeconds(3600),
                SaleStatus.ACTIVE
        );
        when(catalogClient.getSale(SALE_ID)).thenReturn(sale);
        when(paymentClient.charge(any(ChargeRequest.class)))
                .thenAnswer(invocation -> {
                    ChargeRequest request = invocation.getArgument(0);
                    return new ChargeResponse(UUID.randomUUID(), request.orderId(), PaymentStatus.SUCCESS);
                });

        SaleStock stock = new SaleStock();
        stock.setSaleId(SALE_ID);
        stock.setInitialStock(50);
        stock.setSold(0);
        saleStockRepository.save(stock);
        redisStockService.initializeStock(SALE_ID, 50);
    }

    @Test
    void concurrentPurchasesNeverOversell() throws Exception {
        int threads = 100;
        int stock = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Callable<OrderStatus>> tasks = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            int index = i;
            tasks.add(() -> {
                try {
                    return orderService.placeOrder(new OrderService.CreateOrderCommand(
                            SALE_ID,
                            1,
                            "user" + index + "@test.com",
                            "key-" + index
                    )).getStatus();
                } catch (Exception ex) {
                    return OrderStatus.CANCELLED;
                }
            });
        }

        List<Future<OrderStatus>> results = executor.invokeAll(tasks);
        executor.shutdown();

        long confirmed = results.stream()
                .mapToLong(future -> {
                    try {
                        return future.get() == OrderStatus.CONFIRMED ? 1 : 0;
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();

        SaleStock finalStock = saleStockRepository.findById(SALE_ID).orElseThrow();
        assertThat(finalStock.getSold()).isEqualTo((int) confirmed);
        assertThat(confirmed).isEqualTo(stock);
        assertThat(finalStock.getSold()).isLessThanOrEqualTo(finalStock.getInitialStock());
    }
}
