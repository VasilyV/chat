package com.example.chat.controller;

import com.example.chat.kafka.KafkaProducer;
import com.example.chat.model.Message;
import com.example.chat.redis.RedisPublisher;
import com.example.chat.service.RateLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class MessagingController {

    private static final Logger log = LoggerFactory.getLogger(MessagingController.class);

    private final KafkaProducer producer;
    private final ObjectMapper mapper;
    private final RedisPublisher redisPublisher;
    private final RateLimiterService rateLimiterService;

    public MessagingController(KafkaProducer producer, RedisPublisher redisPublisher, ObjectMapper mapper, RateLimiterService rateLimiterService) {
        this.producer = producer;
        this.redisPublisher = redisPublisher;
        this.mapper = mapper;
        this.rateLimiterService = rateLimiterService;
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(Message message) throws Exception {
        log.debug("WS sendMessage roomId={} sender={}", message.getRoomId(), message.getSender());

        // Simple rate limit: 10 messages per 5 seconds
        if (!rateLimiterService.isAllowed("sendMessage:" + message.getSender(), 10, 5)) {
            log.warn("Rate limit exceeded for sender={}", message.getSender());
            throw new RuntimeException("Rate limit exceeded. Please wait a moment.");
        }

        String json = mapper.writeValueAsString(message);
        var roomId = message.getRoomId();
        producer.sendMessage("chat-messages", json, message.getRoomId());

        String channel = "chat:room:" + roomId;
        String redisPayload = mapper.writeValueAsString(message);
        redisPublisher.publish(channel, redisPayload);
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public Message handleException(Exception exception) {
        log.error("WebSocket exception: {}", exception.getMessage());
        Message error = new Message();
        error.setSender("SYSTEM");
        error.setContent("Error: " + exception.getMessage());
        return error;
    }
}
