package com.flashsale.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.events.OrderEvent;
import com.flashsale.inventory.domain.OutboxEvent;
import com.flashsale.inventory.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void enqueue(UUID aggregateId, OrderEvent event) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setAggregateId(aggregateId);
        outboxEvent.setEventType(event.eventType());
        Map<String, Object> payload = objectMapper.convertValue(event, Map.class);
        payload.put("eventType", event.eventType());
        outboxEvent.setCreatedAt(Instant.now());
        outboxEvent.setPayload(payload);
        outboxRepository.save(outboxEvent);
    }
}
