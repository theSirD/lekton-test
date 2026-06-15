package com.flashsale.catalog.web;

import com.flashsale.common.api.SaleResponse;
import com.flashsale.catalog.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final CatalogService catalogService;

    @GetMapping("/{saleId}")
    public SaleResponse getSale(@PathVariable UUID saleId) {
        return catalogService.getSale(saleId);
    }
}
