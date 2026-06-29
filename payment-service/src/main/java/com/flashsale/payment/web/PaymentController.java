package com.flashsale.payment.web;

import com.flashsale.common.PaymentStatus;
import com.flashsale.common.api.ChargeRequest;
import com.flashsale.common.api.ChargeResponse;
import com.flashsale.common.api.RefundRequest;
import com.flashsale.common.api.RefundResponse;
import com.flashsale.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/charge")
    public ChargeResponse charge(
            @RequestBody ChargeRequest request,
            @RequestHeader(value = "X-Payment-Outcome", required = false) String outcomeHeader
    ) {
        PaymentStatus forced = parseOutcome(outcomeHeader);
        return paymentService.charge(request, forced);
    }

    @PostMapping("/refund")
    public RefundResponse refund(@RequestBody RefundRequest request) {
        return paymentService.refund(request);
    }

    private PaymentStatus parseOutcome(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        return PaymentStatus.valueOf(header.toUpperCase());
    }
}
