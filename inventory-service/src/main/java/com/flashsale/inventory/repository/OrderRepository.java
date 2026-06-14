package com.flashsale.inventory.repository;

import com.flashsale.common.OrderStatus;
import com.flashsale.inventory.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, Instant createdBefore);

    @Query("SELECT COALESCE(SUM(o.quantity), 0) FROM Order o WHERE o.saleId = :saleId AND o.status = 'CONFIRMED'")
    int sumConfirmedQuantity(@Param("saleId") UUID saleId);
}
