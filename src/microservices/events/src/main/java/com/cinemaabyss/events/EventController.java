package com.cinemaabyss.events;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);
    private static final Map<String, String> TOPICS = Map.of(
            "movie", "movie-events",
            "user", "user-events",
            "payment", "payment-events");

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public EventController(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse(true);
    }

    @PostMapping("/{eventType}")
    public ResponseEntity<?> publishEvent(
            @PathVariable String eventType,
            @RequestBody JsonNode data) {
        String topic = TOPICS.get(eventType);
        if (topic == null) {
            return ResponseEntity.notFound().build();
        }
        if (data == null || !data.isObject()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Тело события должно быть JSON объектом"));
        }

        String eventId = UUID.randomUUID().toString();
        String key = eventKey(data, eventId);
        EventEnvelope event = new EventEnvelope(eventId, eventType, Instant.now(), data);
        try {
            String value = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, value).get(5, TimeUnit.SECONDS);
        } catch (Exception error) {
            log.error("Ошибка публикации {} события в Kafka топик {}", eventType, topic, error);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Ошибка отправки события"));
        }

        log.info("Published {} event {} to Kafka topic {}", eventType, eventId, topic);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PublishResponse("success", eventId));
    }

    private static String eventKey(JsonNode data, String fallback) {
        for (String field : new String[]{"movie_id", "user_id", "payment_id"}) {
            JsonNode value = data.get(field);
            if (value != null && !value.isNull()) {
                return value.asText(value.toString());
            }
        }
        return fallback;
    }

    public record HealthResponse(boolean status) {
    }

    public record PublishResponse(String status, String event_id) {
    }

    public record EventEnvelope(String event_id, String event_type, Instant timestamp, JsonNode data) {
    }
}