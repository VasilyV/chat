package com.example.chat.controller;

import com.example.chat.kafka.ChatKafkaProducer;
import com.example.chat.model.ChatMessage;
import com.example.chat.redis.RedisPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private final ChatKafkaProducer producer;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RedisPublisher redisPublisher;

    public ChatController(ChatKafkaProducer producer, RedisPublisher redisPublisher) {
        this.producer = producer;
        this.redisPublisher = redisPublisher;
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(ChatMessage message) throws Exception {
        String json = mapper.writeValueAsString(message);
        var roomId = message.getRoomId();
        producer.sendMessage("chat-messages", json, message.getRoomId());

        String channel = "chat:room:" + roomId;
        String redisPayload = mapper.writeValueAsString(message);
        redisPublisher.publish(channel, redisPayload);
        System.out.println("PUBLISHED to Redis channel = " + channel + " payload=" + redisPayload);
    }
}
