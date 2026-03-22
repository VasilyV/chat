package com.example.chat.redis;

import com.example.chat.persistence.MessageEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class RedisMessageSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageSubscriber.class);


    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper mapper;

    public RedisMessageSubscriber(SimpMessagingTemplate messagingTemplate,
                                  ObjectMapper mapper) {
        this.messagingTemplate = messagingTemplate;
        this.mapper = mapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        log.debug("Redis message received channel={} body={}", channel, message.getBody());
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            String[] parts = channel.split(":");
            String roomId = parts[2];
            MessageEntity chatMessage = mapper.readValue(body, MessageEntity.class);
            messagingTemplate.convertAndSend("/topic/rooms/" + roomId, chatMessage);
        } catch (Exception e) {
            log.error("Failed to process Redis message (channel={})", channel, e);
        }
    }
}

