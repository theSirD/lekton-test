package com.flashsale.notification.repository;

import com.flashsale.notification.domain.ProcessedEvent;
import com.flashsale.notification.domain.ProcessedEventId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEventId> {
}
