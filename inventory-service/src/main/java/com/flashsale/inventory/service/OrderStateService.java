package com.flashsale.inventory.service;

import com.flashsale.common.OrderStatus;
import com.flashsale.common.events.OrderCancelledEvent;
import com.flashsale.common.events.OrderConfirmedEvent;
import com.flashsale.common.api.RefundRequest;
import com.flashsale.inventory.client.PaymentClient;
import com.flashsale.inventory.domain.Order;
import com.flashsale.inventory.redis.RedisStockService;
import com.flashsale.inventory.repository.OrderRepository;
import com.flashsale.inventory.repository.SaleStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrderStateService {

    private final OrderRepository orderRepository;
    private final SaleStockRepository saleStockRepository;
    private final PaymentClient paymentClient;
    private final RedisStockService redisStockService;
    private final OutboxService outboxService;

    @Transactional
    public Order createPending(Order order) {
        return orderRepository.save(order);
    }

    @Transactional
    public Order confirm(Order order) {
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            return order;
        }
        int updated = saleStockRepository.incrementSoldIfAvailable(order.getSaleId(), order.getQuantity());
        if (updated == 0) {
            paymentClient.refund(new RefundRequest(order.getId()));
            redisStockService.consumeReserve(order.getId());
            redisStockService.adjustStock(order.getSaleId(), order.getQuantity());
            return cancel(order, true);
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);

        outboxService.enqueue(order.getId(), new OrderConfirmedEvent(
                order.getId(),
                order.getUserEmail(),
                order.getSaleId(),
                order.getQuantity(),
                order.getAmount(),
                Instant.now()
        ));

        redisStockService.consumeReserve(order.getId());
        return order;
    }

    @Transactional
    public Order cancel(Order order, boolean stockAlreadyReturned) {
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.EXPIRED) {
            return order;
        }
        if (!stockAlreadyReturned) {
            redisStockService.releaseReserve(order.getSaleId(), order.getId());
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);

        outboxService.enqueue(order.getId(), new OrderCancelledEvent(
                order.getId(),
                order.getUserEmail(),
                order.getSaleId(),
                order.getQuantity(),
                order.getAmount(),
                Instant.now()
        ));
        return order;
    }

    @Transactional
    public void expire(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }
        redisStockService.releaseReserve(order.getSaleId(), order.getId());
        order.setStatus(OrderStatus.EXPIRED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);

        outboxService.enqueue(order.getId(), new OrderCancelledEvent(
                order.getId(),
                order.getUserEmail(),
                order.getSaleId(),
                order.getQuantity(),
                order.getAmount(),
                Instant.now()
        ));
    }
}
