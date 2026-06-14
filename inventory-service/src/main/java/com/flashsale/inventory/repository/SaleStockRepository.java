package com.flashsale.inventory.repository;

import com.flashsale.inventory.domain.SaleStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SaleStockRepository extends JpaRepository<SaleStock, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE SaleStock s
            SET s.sold = s.sold + :quantity
            WHERE s.saleId = :saleId AND s.sold + :quantity <= s.initialStock
            """)
    int incrementSoldIfAvailable(@Param("saleId") UUID saleId, @Param("quantity") int quantity);
}
