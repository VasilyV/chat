package com.example.chat.kafka;

import com.example.chat.service.ChatMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatKafkaConsumerTest {

    @Mock ChatMessageService chatMessageService;
    @Mock KafkaTemplate<String, String> kafkaTemplate;
    @Mock StringRedisTemplate redisTemplate;
    @Mock Acknowledgment ack;

    @Test
    void listen_shouldPersistAndAcknowledge_onHappyPath() throws Exception {
        ChatKafkaConsumer consumer = new ChatKafkaConsumer(
                new ObjectMapper(),
                chatMessageService,
                kafkaTemplate
        );

        String payload = "{\"roomId\":\"room1\",\"sender\":\"bob\",\"content\":\"hi\"}";
        consumer.listen(payload, ack);

        verify(chatMessageService).save("room1", "bob", "hi");
        verify(ack).acknowledge();
        verify(kafkaTemplate, never()).send(eq("chat-messages-dlq"), anyString());
    }

    @Test
    void listen_shouldSendToDlq_whenPayloadIsBad() throws Exception {
        ChatKafkaConsumer consumer = new ChatKafkaConsumer(
                new ObjectMapper(),
                chatMessageService,
                kafkaTemplate
        );

        String payload = "{\"roomId\":\"room1\",\"content\":\"hi\"}";
        consumer.listen(payload, ack);

        verify(kafkaTemplate).send("chat-messages-dlq", payload);
        verify(ack, never()).acknowledge();
        verify(chatMessageService, never()).save(anyString(), anyString(), anyString());
    }
}
