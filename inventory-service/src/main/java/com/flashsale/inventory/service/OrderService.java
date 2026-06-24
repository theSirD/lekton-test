package com.flashsale.inventory.service;

import com.flashsale.common.OrderStatus;
import com.flashsale.common.SaleStatus;
import com.flashsale.common.api.SaleResponse;
import com.flashsale.inventory.client.CatalogClient;
import com.flashsale.inventory.domain.Order;
import com.flashsale.inventory.redis.RedisStockService;
import com.flashsale.inventory.repository.OrderRepository;
import com.flashsale.inventory.repository.SaleStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CatalogClient catalogClient;
    private final RedisStockService redisStockService;
    private final SaleStockRepository saleStockRepository;
    private final SaleStockInitializer saleStockInitializer;

    @Value("${inventory.reserve-ttl-seconds:600}")
    private long reserveTtlSeconds;

    public Order placeOrder(CreateOrderCommand command) {
        return orderRepository.findByIdempotencyKey(command.idempotencyKey())
                .orElseGet(() -> createOrder(command));
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
                .forEach(this::expireOrder);
    }

    private Order createOrder(CreateOrderCommand command) {
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
            order = savePending(order);
        } catch (RuntimeException ex) {
            redisStockService.releaseReserve(sale.id(), orderId);
            throw ex;
        }

        return confirmOrder(order);
    }

    @Transactional
    protected Order savePending(Order order) {
        return orderRepository.save(order);
    }

    @Transactional
    protected Order confirmOrder(Order order) {
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            return order;
        }
        int updated = saleStockRepository.incrementSoldIfAvailable(order.getSaleId(), order.getQuantity());
        if (updated == 0) {
            redisStockService.releaseReserve(order.getSaleId(), order.getId());
            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(Instant.now());
            orderRepository.save(order);
            return order;
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        redisStockService.consumeReserve(order.getId());
        return order;
    }

    @Transactional
    protected void expireOrder(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }
        redisStockService.releaseReserve(order.getSaleId(), order.getId());
        order.setStatus(OrderStatus.EXPIRED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
    }

    public record CreateOrderCommand(UUID saleId, int quantity, String userEmail, String idempotencyKey) {
    }

    public record SaleView(SaleResponse sale, int availableStock, SaleStatus status) {
    }
}
