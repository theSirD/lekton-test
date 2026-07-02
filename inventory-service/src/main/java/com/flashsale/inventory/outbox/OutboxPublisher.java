package com.flashsale.inventory.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.Topics;
import com.flashsale.inventory.domain.OutboxEvent;
import com.flashsale.inventory.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${inventory.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository.findUnpublishedForUpdate();
        for (OutboxEvent event : events) {
            try {
                String payload = objectMapper.writeValueAsString(event.getPayload());
                kafkaTemplate.send(Topics.ORDER_EVENTS, event.getAggregateId().toString(), payload);
                event.setPublishedAt(Instant.now());
            } catch (Exception ex) {
                log.error("Failed to publish outbox event {}", event.getId(), ex);
            }
        }
    }
}
