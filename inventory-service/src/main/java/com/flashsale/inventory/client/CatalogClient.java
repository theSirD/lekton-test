package com.flashsale.inventory.client;

import com.flashsale.common.api.SaleResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(@Value("${clients.catalog.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public SaleResponse getSale(UUID saleId) {
        return restClient.get()
                .uri("/api/sales/{saleId}", saleId)
                .retrieve()
                .body(SaleResponse.class);
    }
}
