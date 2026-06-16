package com.flashsale.inventory.web;

import com.flashsale.inventory.domain.Order;
import com.flashsale.inventory.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/sales/{saleId}")
    public OrderService.SaleView getSale(@PathVariable UUID saleId) {
        return orderService.getSaleView(saleId);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
            @RequestBody CreateOrderRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        Order order = orderService.placeOrder(new OrderService.CreateOrderCommand(
                request.saleId(),
                request.quantity(),
                request.userEmail(),
                idempotencyKey
        ));
        return OrderResponse.from(order);
    }

    @GetMapping("/orders/{orderId}")
    public OrderResponse getOrder(@PathVariable UUID orderId) {
        return OrderResponse.from(orderService.getOrder(orderId));
    }
}
