package com.example.chat.redis;

import com.example.chat.persistence.ChatMessageEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisMessageSubscriberTest {

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @Captor
    ArgumentCaptor<String> destinationCaptor;

    @Captor
    ArgumentCaptor<Object> payloadCaptor;

    @Test
    void onMessage_shouldParseChannelRoomId_andForwardToWebsocketTopic() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RedisMessageSubscriber subscriber = new RedisMessageSubscriber(messagingTemplate, mapper);

        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setRoomId("room-7");
        entity.setSender("alice");
        entity.setContent("hello");
        entity.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));

        String body = mapper.writeValueAsString(entity);
        Message msg = new DefaultMessage(
                "chat:room:room-7".getBytes(StandardCharsets.UTF_8),
                body.getBytes(StandardCharsets.UTF_8)
        );

        subscriber.onMessage(msg, null);

        verify(messagingTemplate).convertAndSend(destinationCaptor.capture(), payloadCaptor.capture());
        assertThat(destinationCaptor.getValue()).isEqualTo("/topic/rooms/room-7");

        Object payload = payloadCaptor.getValue();
        assertThat(payload).isInstanceOf(ChatMessageEntity.class);
        ChatMessageEntity forwarded = (ChatMessageEntity) payload;
        assertThat(forwarded.getRoomId()).isEqualTo("room-7");
        assertThat(forwarded.getSender()).isEqualTo("alice");
        assertThat(forwarded.getContent()).isEqualTo("hello");
    }
}
