package com.flashsale.inventory.service;

import com.flashsale.common.OrderStatus;
import com.flashsale.common.SaleStatus;
import com.flashsale.common.api.SaleResponse;
import com.flashsale.inventory.client.CatalogClient;
import com.flashsale.inventory.domain.Order;
import com.flashsale.inventory.domain.SaleStock;
import com.flashsale.inventory.repository.OrderRepository;
import com.flashsale.inventory.repository.SaleStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CatalogClient catalogClient;
    private final SaleStockRepository saleStockRepository;

    public Order placeOrder(CreateOrderCommand command) {
        SaleResponse sale = catalogClient.getSale(command.saleId());
        if (sale.status() != SaleStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sale is not active");
        }

        ensureSaleStock(sale);

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setSaleId(sale.id());
        order.setQuantity(command.quantity());
        order.setStatus(OrderStatus.PENDING);
        order.setAmount(sale.price().multiply(BigDecimal.valueOf(command.quantity())));
        order.setUserEmail(command.userEmail());
        order.setIdempotencyKey(command.idempotencyKey());
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        return orderRepository.save(order);
    }

    public Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    public SaleView getSaleView(UUID saleId) {
        SaleResponse sale = catalogClient.getSale(saleId);
        ensureSaleStock(sale);
        SaleStock stock = saleStockRepository.findById(saleId).orElseThrow();
        int available = stock.getInitialStock() - stock.getSold();
        SaleStatus status = available <= 0 && sale.status() == SaleStatus.ACTIVE
                ? SaleStatus.SOLD_OUT
                : sale.status();
        return new SaleView(sale, available, status);
    }

    private void ensureSaleStock(SaleResponse sale) {
        saleStockRepository.findById(sale.id()).orElseGet(() -> {
            SaleStock created = new SaleStock();
            created.setSaleId(sale.id());
            created.setInitialStock(sale.initialStock());
            created.setSold(0);
            return saleStockRepository.save(created);
        });
    }

    public record CreateOrderCommand(UUID saleId, int quantity, String userEmail, String idempotencyKey) {
    }

    public record SaleView(SaleResponse sale, int availableStock, SaleStatus status) {
    }
}
