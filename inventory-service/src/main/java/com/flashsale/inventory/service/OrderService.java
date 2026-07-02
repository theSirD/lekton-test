package com.flashsale.inventory.service;

import com.flashsale.common.OrderStatus;
import com.flashsale.common.SaleStatus;
import com.flashsale.common.api.SaleResponse;
import com.flashsale.inventory.client.CatalogClient;
import com.flashsale.inventory.domain.Order;
import com.flashsale.inventory.redis.RedisStockService;
import com.flashsale.inventory.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CatalogClient catalogClient;
    private final RedisStockService redisStockService;
    private final OrderStateService orderStateService;
    private final OrderCreationService orderCreationService;
    private final SaleStockInitializer saleStockInitializer;

    @Value("${inventory.reserve-ttl-seconds:600}")
    private long reserveTtlSeconds;

    public Order placeOrder(CreateOrderCommand command) {
        return orderRepository.findByIdempotencyKey(command.idempotencyKey())
                .map(this::resumeIfNeeded)
                .orElseGet(() -> orderCreationService.createOrder(command));
    }

    public Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    public SaleView getSaleView(UUID saleId) {
        SaleResponse sale = catalogClient.getSale(saleId);
        saleStockInitializer.ensureInitialized(sale);
        int available = redisStockService.getAvailableStock(saleId);
        SaleStatus status = available <= 0 && sale.status() == SaleStatus.ACTIVE
                ? SaleStatus.SOLD_OUT
                : sale.status();
        return new SaleView(sale, available, status);
    }

    public void expireStaleOrders() {
        Instant cutoff = Instant.now().minusSeconds(reserveTtlSeconds);
        orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, cutoff)
                .forEach(orderStateService::expire);
    }

    private Order resumeIfNeeded(Order order) {
        if (order.getStatus() == OrderStatus.PENDING) {
            return orderCreationService.completePendingOrder(order, null);
        }
        return order;
    }

    public record CreateOrderCommand(UUID saleId, int quantity, String userEmail, String idempotencyKey) {
    }

    public record SaleView(SaleResponse sale, int availableStock, SaleStatus status) {
    }
}
