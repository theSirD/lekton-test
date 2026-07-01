package com.flashsale.inventory.client;

import com.flashsale.common.api.ChargeRequest;
import com.flashsale.common.api.ChargeResponse;
import com.flashsale.common.api.RefundRequest;
import com.flashsale.common.api.RefundResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PaymentClient {

    private final RestClient restClient;

    public PaymentClient(@Value("${clients.payment.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public ChargeResponse charge(ChargeRequest request) {
        return restClient.post()
                .uri("/api/payments/charge")
                .body(request)
                .retrieve()
                .body(ChargeResponse.class);
    }

    public ChargeResponse chargeWithOutcome(ChargeRequest request, String outcome) {
        return restClient.post()
                .uri("/api/payments/charge")
                .header("X-Payment-Outcome", outcome)
                .body(request)
                .retrieve()
                .body(ChargeResponse.class);
    }

    public RefundResponse refund(RefundRequest request) {
        return restClient.post()
                .uri("/api/payments/refund")
                .body(request)
                .retrieve()
                .body(RefundResponse.class);
    }
}
