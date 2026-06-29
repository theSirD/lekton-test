package com.flashsale.payment.service;

import com.flashsale.common.PaymentStatus;
import com.flashsale.common.api.ChargeRequest;
import com.flashsale.common.api.ChargeResponse;
import com.flashsale.common.api.RefundRequest;
import com.flashsale.common.api.RefundResponse;
import com.flashsale.payment.domain.Payment;
import com.flashsale.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Value("${payment.default-outcome:SUCCESS}")
    private PaymentStatus defaultOutcome;

    @Transactional
    public ChargeResponse charge(ChargeRequest request, PaymentStatus forcedOutcome) {
        return paymentRepository.findByOrderId(request.orderId())
                .map(existing -> new ChargeResponse(existing.getId(), existing.getOrderId(), existing.getStatus()))
                .orElseGet(() -> createPayment(request, forcedOutcome));
    }

    @Transactional
    public RefundResponse refund(RefundRequest request) {
        Payment payment = paymentRepository.findByOrderId(request.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setUpdatedAt(Instant.now());
        }
        return new RefundResponse(payment.getOrderId(), payment.getStatus());
    }

    private ChargeResponse createPayment(ChargeRequest request, PaymentStatus forcedOutcome) {
        PaymentStatus outcome = forcedOutcome != null ? forcedOutcome : defaultOutcome;
        if (outcome == PaymentStatus.TIMEOUT) {
            outcome = PaymentStatus.FAILED;
        }
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(request.orderId());
        payment.setAmount(request.amount());
        payment.setStatus(outcome);
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());
        paymentRepository.save(payment);
        return new ChargeResponse(payment.getId(), payment.getOrderId(), payment.getStatus());
    }
}
