package com.flashsale.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_event")
@IdClass(ProcessedEventId.class)
@Getter
@Setter
public class ProcessedEvent {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @Id
    @Column(name = "event_type")
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
