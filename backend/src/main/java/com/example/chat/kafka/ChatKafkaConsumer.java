package com.example.chat.kafka;

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
    private final StringRedisTemplate redisTemplate;

    public ChatKafkaConsumer(ObjectMapper mapper, ChatMessageService chatMessageService, KafkaTemplate<String, String> kafkaTemplate, StringRedisTemplate redisTemplate) {
        this.mapper = mapper;
        this.chatMessageService = chatMessageService;
        this.kafkaTemplate = kafkaTemplate;
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(topics = "chat-messages", groupId = "chat-backend-group")
    public void listen(String payload, Acknowledgment ack) throws Exception {
        try {
            var node = mapper.readTree(payload);
            String roomId = node.get("roomId").asText();
            String sender = node.get("sender").asText();
            String content = node.get("content").asText();

            var saved = chatMessageService.save(roomId, sender, content);
            System.out.println("Saved to DB: " + saved);

            ack.acknowledge();
        } catch (Exception e) {
            System.out.println("Error while listening to chat-messages: " + e.getMessage());
            kafkaTemplate.send("chat-messages-dlq", payload);
        }

    }
}
