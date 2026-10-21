package com.cinemaabyss.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    @KafkaListener(topicPattern = "movie-events|user-events|payment-events")
    public void consume(ConsumerRecord<String, String> record) {
        log.info("Обработка события из топика {}: {}", record.topic(), record.value());
    }
}