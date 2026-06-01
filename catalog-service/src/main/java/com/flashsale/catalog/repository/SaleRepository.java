package com.flashsale.catalog.repository;

import com.flashsale.catalog.domain.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID> {
}
