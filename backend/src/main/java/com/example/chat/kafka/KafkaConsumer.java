package com.example.chat.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.chat.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    private final ObjectMapper mapper;
    private final MessageService messageService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaConsumer(ObjectMapper mapper, MessageService messageService, KafkaTemplate<String, String> kafkaTemplate) {
        this.mapper = mapper;
        this.messageService = messageService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "chat-messages", groupId = "chat-backend-group")
    public void listen(String payload, Acknowledgment ack) {
        log.debug("Kafka received bytes={}", payload == null ? 0 : payload.length());
        try {
            save(payload);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Kafka consume failed; sending to DLQ");
            kafkaTemplate.send("chat-messages-dlq", payload);
        }
    }

    private void save (String payload) throws JsonProcessingException {
        var node = mapper.readTree(payload);
        String roomId = node.get("roomId").asText();
        String sender = node.get("sender").asText();
        String content = node.get("content").asText();
        messageService.save(roomId, sender, content);
        log.debug("Saved message roomId={} sender={}", roomId, sender);
    }
}
