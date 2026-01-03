package com.example.chat.controller;

import com.example.chat.kafka.ChatKafkaProducer;
import com.example.chat.model.ChatMessage;
import com.example.chat.redis.RedisPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatKafkaProducer producer;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RedisPublisher redisPublisher;

    public ChatController(ChatKafkaProducer producer, RedisPublisher redisPublisher) {
        this.producer = producer;
        this.redisPublisher = redisPublisher;
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(ChatMessage message) throws Exception {
        log.debug("WS sendMessage roomId={} sender={}", message.getRoomId(), message.getSender());
        String json = mapper.writeValueAsString(message);
        var roomId = message.getRoomId();
        producer.sendMessage("chat-messages", json, message.getRoomId());

        String channel = "chat:room:" + roomId;
        String redisPayload = mapper.writeValueAsString(message);
        redisPublisher.publish(channel, redisPayload);
    }
}
