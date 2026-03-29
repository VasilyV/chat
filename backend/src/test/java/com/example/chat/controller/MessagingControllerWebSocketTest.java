package com.example.chat.controller;

import com.example.chat.config.WebSocketConfig;
import com.example.chat.kafka.KafkaProducer;
import com.example.chat.model.Message;
import com.example.chat.redis.RedisPublisher;
import com.example.chat.service.MessageService;
import com.example.chat.service.RateLimiterService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.messaging.simp.stomp.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = MessagingControllerWebSocketTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.flyway.enabled=false",
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration," +
                        "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
        }
)
class MessagingControllerWebSocketTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({ MessagingController.class, WebSocketConfig.class })
    static class TestApp { }

    @LocalServerPort
    int port;

    @MockBean
    KafkaProducer producer;

    @MockBean
    RedisPublisher redisPublisher;

    @MockBean
    MessageService messageService;

    @MockBean
    RateLimiterService rateLimiterService;

    private WebSocketStompClient stompClient;
    private StompSession session;

    @AfterEach
    void tearDown() {
        try {
            if (session != null && session.isConnected()) session.disconnect();
        } catch (Exception ignored) {}
        try {
            if (stompClient != null) stompClient.stop();
        } catch (Exception ignored) {}
    }

    @Test
    void sendMessage_overWebSocket_shouldSendToKafka_andPublishToRedis() throws Exception {
        when(rateLimiterService.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(true);

        CountDownLatch latch = new CountDownLatch(2);
        doAnswer(inv -> { latch.countDown(); return null; })
                .when(producer).sendMessage(anyString(), anyString(), anyString());
        doAnswer(inv -> { latch.countDown(); return null; })
                .when(redisPublisher).publish(anyString(), anyString());

        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        String url = "ws://localhost:" + port + "/ws-chat";
        session = stompClient
                .connectAsync(url, new StompSessionHandlerAdapter() {})
                .get(3, TimeUnit.SECONDS);

        Message m = new Message();
        m.setRoomId("room-1");
        m.setSender("alice");
        m.setContent("hello");

        session.send("/app/chat.sendMessage", m);

        if (!latch.await(3, TimeUnit.SECONDS)) {
            fail("Timed out waiting for Kafka + Redis calls");
        }

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        verify(producer, times(1))
                .sendMessage(topicCaptor.capture(), payloadCaptor.capture(), keyCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("chat-messages");
        assertThat(keyCaptor.getValue()).isEqualTo("room-1");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode sent = mapper.readTree(payloadCaptor.getValue());
        assertThat(sent.get("roomId").asText()).isEqualTo("room-1");
        assertThat(sent.get("sender").asText()).isEqualTo("alice");
        assertThat(sent.get("content").asText()).isEqualTo("hello");

        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> redisPayloadCaptor = ArgumentCaptor.forClass(String.class);

        verify(redisPublisher, times(1))
                .publish(channelCaptor.capture(), redisPayloadCaptor.capture());

        assertThat(channelCaptor.getValue()).isEqualTo("chat:room:room-1");

        JsonNode redis = mapper.readTree(redisPayloadCaptor.getValue());
        assertThat(redis.get("roomId").asText()).isEqualTo("room-1");
        assertThat(redis.get("sender").asText()).isEqualTo("alice");
        assertThat(redis.get("content").asText()).isEqualTo("hello");
    }

    @Test
    void sendMessage_whenRateLimited_shouldSendErrorMessageToUser() throws Exception {
        when(rateLimiterService.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(false);

        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        String url = "ws://localhost:" + port + "/ws-chat";
        
        CountDownLatch errorLatch = new CountDownLatch(1);
        final Message[] errorReceived = new Message[1];

        session = stompClient
                .connectAsync(url, new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                        session.subscribe("/user/queue/errors", new StompFrameHandler() {
                            @Override
                            public java.lang.reflect.Type getPayloadType(StompHeaders headers) {
                                return Message.class;
                            }

                            @Override
                            public void handleFrame(StompHeaders headers, Object payload) {
                                errorReceived[0] = (Message) payload;
                                errorLatch.countDown();
                            }
                        });
                    }
                })
                .get(3, TimeUnit.SECONDS);

        Message m = new Message();
        m.setRoomId("room-1");
        m.setSender("bob");
        m.setContent("spam");

        session.send("/app/chat.sendMessage", m);

        if (!errorLatch.await(3, TimeUnit.SECONDS)) {
            fail("Timed out waiting for error message on /user/queue/errors");
        }

        assertThat(errorReceived[0]).isNotNull();
        assertThat(errorReceived[0].getSender()).isEqualTo("SYSTEM");
        assertThat(errorReceived[0].getContent()).contains("Rate limit exceeded");

        verify(producer, never()).sendMessage(anyString(), anyString(), anyString());
        verify(redisPublisher, never()).publish(anyString(), anyString());
    }
}
