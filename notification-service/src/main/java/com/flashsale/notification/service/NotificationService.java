package com.flashsale.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.events.OrderCancelledEvent;
import com.flashsale.common.events.OrderConfirmedEvent;
import com.flashsale.common.events.OrderEvent;
import com.flashsale.notification.domain.ProcessedEvent;
import com.flashsale.notification.domain.ProcessedEventId;
import com.flashsale.notification.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final ProcessedEventRepository processedEventRepository;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handleEvent(String payload) {
        try {
            OrderEvent event = objectMapper.readValue(payload, OrderEvent.class);
            String eventType = event.eventType();

            if (processedEventRepository.existsById(new ProcessedEventId(event.orderId(), eventType))) {
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.userEmail());
            message.setSubject(buildSubject(event));
            message.setText(buildBody(event));
            mailSender.send(message);

            ProcessedEvent processed = new ProcessedEvent();
            processed.setOrderId(event.orderId());
            processed.setEventType(eventType);
            processed.setProcessedAt(Instant.now());
            processedEventRepository.save(processed);
            log.info("Notification sent for order {} ({})", event.orderId(), eventType);
        } catch (Exception ex) {
            log.error("Failed to process notification event", ex);
            throw new IllegalStateException("Notification processing failed", ex);
        }
    }

    private String buildSubject(OrderEvent event) {
        return switch (event) {
            case OrderConfirmedEvent ignored -> "Order confirmed";
            case OrderCancelledEvent ignored -> "Order cancelled";
            default -> "Order update";
        };
    }

    private String buildBody(OrderEvent event) {
        String summary = "Order %s: %d item(s), total %s RUB."
                .formatted(event.orderId(), event.quantity(), event.amount());
        return switch (event) {
            case OrderConfirmedEvent ignored -> "Your purchase is confirmed. " + summary;
            case OrderCancelledEvent ignored -> "Your order was cancelled. " + summary;
            default -> summary;
        };
    }
}
