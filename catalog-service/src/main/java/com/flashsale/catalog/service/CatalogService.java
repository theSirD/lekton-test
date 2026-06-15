package com.flashsale.catalog.service;

import com.flashsale.common.api.SaleResponse;
import com.flashsale.catalog.domain.Sale;
import com.flashsale.catalog.repository.SaleRepository;
import com.flashsale.common.SaleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final SaleRepository saleRepository;

    public SaleResponse getSale(UUID saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sale not found"));
        return toResponse(sale);
    }

    public SaleResponse toResponse(Sale sale) {
        Instant now = Instant.now();
        SaleStatus status = resolveStatus(sale, now);
        return new SaleResponse(
                sale.getId(),
                sale.getProduct().getId(),
                sale.getProduct().getName(),
                sale.getProduct().getDescription(),
                sale.getPrice(),
                sale.getInitialStock(),
                sale.getStartsAt(),
                sale.getEndsAt(),
                status
        );
    }

    public void validateSaleActive(Sale sale) {
        Instant now = Instant.now();
        SaleStatus status = resolveStatus(sale, now);
        if (status != SaleStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sale is not active: " + status);
        }
    }

    public Sale requireSale(UUID saleId) {
        return saleRepository.findById(saleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sale not found"));
    }

    private SaleStatus resolveStatus(Sale sale, Instant now) {
        if (now.isBefore(sale.getStartsAt())) {
            return SaleStatus.NOT_STARTED;
        }
        if (now.isAfter(sale.getEndsAt())) {
            return SaleStatus.ENDED;
        }
        return SaleStatus.ACTIVE;
    }
}
