package com.example.chat.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ChatKafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, String message, String roomId) {
        kafkaTemplate.send(topic, roomId, message);
    }
}
