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
            System.out.println("REDIS SUB message received — raw channel = " +
                    new String(message.getChannel()));

            System.out.println("REDIS SUB body = " +
                    new String(message.getBody()));
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String body = new String(message.getBody(), StandardCharsets.UTF_8);

            String[] parts = channel.split(":");
            String roomId = parts[2];

            ChatMessageEntity chatMessage = mapper.readValue(body, ChatMessageEntity.class);

            messagingTemplate.convertAndSend("/topic/rooms/" + roomId, chatMessage);
            System.out.println("Message sent to room: " + roomId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

