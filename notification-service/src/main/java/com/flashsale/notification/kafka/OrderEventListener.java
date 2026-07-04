package com.flashsale.notification.kafka;

import com.flashsale.common.Topics;
import com.flashsale.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = Topics.ORDER_EVENTS, groupId = "notification-service")
    public void onMessage(String payload) {
        notificationService.handleEvent(payload);
    }
}
