package com.flashsale.inventory.scheduler;

import com.flashsale.inventory.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderExpiryScheduler {

    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${inventory.expiry.poll-interval-ms:5000}")
    public void expireOrders() {
        orderService.expireStaleOrders();
    }
}
