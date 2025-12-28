package com.example.chat.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.chat.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ChatKafkaConsumer {

    private final ObjectMapper mapper;
    private final ChatMessageService chatMessageService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ChatKafkaConsumer(ObjectMapper mapper, ChatMessageService chatMessageService, KafkaTemplate<String, String> kafkaTemplate) {
        this.mapper = mapper;
        this.chatMessageService = chatMessageService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "chat-messages", groupId = "chat-backend-group")
    public void listen(String payload, Acknowledgment ack) throws Exception {
        try {
            save(payload);
            ack.acknowledge();
        } catch (Exception e) {
            System.out.println("Error while listening to chat-messages: " + e.getMessage());
            kafkaTemplate.send("chat-messages-dlq", payload);
        }

    }

    private void save (String payload) throws JsonProcessingException {
        var node = mapper.readTree(payload);
        String roomId = node.get("roomId").asText();
        String sender = node.get("sender").asText();
        String content = node.get("content").asText();
        chatMessageService.save(roomId, sender, content);
    }
}
