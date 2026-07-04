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

            if (event instanceof OrderConfirmedEvent) {
                message.setSubject("Order confirmed");
                message.setText("Your purchase is confirmed. Order " + event.orderId()
                        + ": " + event.quantity() + " item(s), total " + event.amount() + " RUB.");
            } else if (event instanceof OrderCancelledEvent) {
                message.setSubject("Order cancelled");
                message.setText("Your order was cancelled. Order " + event.orderId()
                        + ": " + event.quantity() + " item(s), total " + event.amount() + " RUB.");
            } else {
                message.setSubject("Order update");
                message.setText("Order " + event.orderId()
                        + ": " + event.quantity() + " item(s), total " + event.amount() + " RUB.");
            }

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
}
