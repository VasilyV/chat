package com.example.chat.redis;

import com.example.chat.model.ChatMessage;
import com.example.chat.persistence.ChatMessageEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class RedisMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper mapper;

    public RedisMessageSubscriber(SimpMessagingTemplate messagingTemplate,
                                  ObjectMapper mapper) {
        this.messagingTemplate = messagingTemplate;
        this.mapper = mapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String body = new String(message.getBody(), StandardCharsets.UTF_8);

            String[] parts = channel.split(":");
            String roomId = parts[2];

            ChatMessageEntity chatMessage = mapper.readValue(body, ChatMessageEntity.class);

            messagingTemplate.convertAndSend("/topic/rooms/" + roomId, chatMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

