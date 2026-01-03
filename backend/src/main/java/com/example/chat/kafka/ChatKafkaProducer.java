package com.example.chat.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(ChatKafkaProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ChatKafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, String message, String roomId) {
        log.debug("Kafka send to topic={}, message={}", topic, message);
        kafkaTemplate.send(topic, roomId, message).handle((result, exception) -> {
            if (result != null) {
                log.debug("Kafka send OK topic={} messsage={}", topic, message);
                return result;
            } else {
                log.error("Kafka send FAILED topic={} key={}", topic, message, exception);
                throw new RuntimeException(exception);
            }
        });
    }
}
