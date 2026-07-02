package com.flashsale.inventory.service;

import com.flashsale.common.OrderStatus;
import com.flashsale.common.PaymentStatus;
import com.flashsale.common.SaleStatus;
import com.flashsale.common.api.ChargeRequest;
import com.flashsale.common.api.ChargeResponse;
import com.flashsale.common.api.SaleResponse;
import com.flashsale.inventory.client.CatalogClient;
import com.flashsale.inventory.client.PaymentClient;
import com.flashsale.inventory.domain.Order;
import com.flashsale.inventory.redis.RedisStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderCreationService {

    private final CatalogClient catalogClient;
    private final PaymentClient paymentClient;
    private final RedisStockService redisStockService;
    private final OrderStateService orderStateService;
    private final SaleStockInitializer saleStockInitializer;

    @Value("${inventory.reserve-ttl-seconds:600}")
    private long reserveTtlSeconds;

    public Order createOrder(OrderService.CreateOrderCommand command) {
        SaleResponse sale = catalogClient.getSale(command.saleId());
        if (sale.status() != SaleStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sale is not active");
        }
        saleStockInitializer.ensureInitialized(sale);

        UUID orderId = UUID.randomUUID();
        if (!redisStockService.tryReserve(sale.id(), orderId, command.quantity(), reserveTtlSeconds)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sold out");
        }

        Order order = new Order();
        order.setId(orderId);
        order.setSaleId(sale.id());
        order.setQuantity(command.quantity());
        order.setStatus(OrderStatus.PENDING);
        order.setAmount(sale.price().multiply(BigDecimal.valueOf(command.quantity())));
        order.setUserEmail(command.userEmail());
        order.setIdempotencyKey(command.idempotencyKey());
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        try {
            order = orderStateService.createPending(order);
        } catch (RuntimeException ex) {
            redisStockService.releaseReserve(sale.id(), orderId);
            throw ex;
        }

        return completePendingOrder(order, null);
    }

    public Order completePendingOrder(Order order, String paymentOutcome) {
        try {
            ChargeResponse paymentResponse = paymentOutcome == null
                    ? paymentClient.charge(new ChargeRequest(order.getId(), order.getAmount()))
                    : paymentClient.chargeWithOutcome(new ChargeRequest(order.getId(), order.getAmount()), paymentOutcome);

            if (paymentResponse.status() == PaymentStatus.SUCCESS) {
                return orderStateService.confirm(order);
            }
            return orderStateService.cancel(order, false);
        } catch (RuntimeException ex) {
            return orderStateService.cancel(order, false);
        }
    }
}
