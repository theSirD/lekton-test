package com.flashsale.inventory;

import com.flashsale.common.OrderStatus;
import com.flashsale.common.PaymentStatus;
import com.flashsale.common.api.ChargeRequest;
import com.flashsale.common.api.ChargeResponse;
import com.flashsale.inventory.domain.SaleStock;
import com.flashsale.inventory.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PaymentFailureIntegrationTest extends AbstractIntegrationTest {

    private static final UUID SALE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        saleStockRepository.deleteAll();

        when(catalogClient.getSale(SALE_ID)).thenReturn(TestSaleFixtures.activeSale(SALE_ID, 10));
        when(paymentClient.charge(any(ChargeRequest.class)))
                .thenReturn(new ChargeResponse(UUID.randomUUID(), UUID.randomUUID(), PaymentStatus.FAILED));

        saleStockRepository.save(TestSaleFixtures.saleStock(SALE_ID, 10));
        redisStockService.initializeStock(SALE_ID, 10);
    }

    @Test
    void failedPaymentReturnsStock() {
        var order = orderService.placeOrder(new OrderService.CreateOrderCommand(
                SALE_ID, 2, "user@test.com", "idem-1"
        ));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        SaleStock stock = saleStockRepository.findById(SALE_ID).orElseThrow();
        assertThat(stock.getSold()).isZero();
        assertThat(redisStockService.getAvailableStock(SALE_ID)).isEqualTo(10);
    }
}
