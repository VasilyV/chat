package com.example.chat.service;

import com.example.chat.model.Message;
import com.example.chat.persistence.MessageEntity;
import com.example.chat.persistence.MessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    MessageRepository messageRepository;

    @InjectMocks
    MessageService messageService;

    @Captor
    ArgumentCaptor<MessageEntity> entityCaptor;

    @Captor
    ArgumentCaptor<Pageable> pageableCaptor;

    @Test
    void save_shouldPopulateEntityAndDelegateToRepository() {
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MessageEntity expectedMessage = new MessageEntity();
        expectedMessage.setRoomId("room-1");
        expectedMessage.setSender("alice");
        expectedMessage.setContent("hello");

        messageService.save("room-1", "alice", "hello");

        verify(messageRepository).save(entityCaptor.capture());
        MessageEntity actualMessage = entityCaptor.getValue();

        assertThat(actualMessage).usingRecursiveComparison().isEqualTo(expectedMessage);
    }

    @Test
    void getMessages_whenNoCursor_shouldUseFindLatestAndMapToModel() {
        Instant createdAt = Instant.parse("2025-01-01T00:00:00Z");
        MessageEntity message = new MessageEntity();
        message.setRoomId("room-1");
        message.setSender("alice");
        message.setContent("hi");
        message.setCreatedAt(createdAt);
        Message expectedMessage = new Message();
        expectedMessage.setRoomId("room-1");
        expectedMessage.setSender("alice");
        expectedMessage.setContent("hi");
        expectedMessage.setCreatedAt(createdAt);

        when(messageRepository.findLatestByRoomId(eq("room-1"), any(Pageable.class))).thenReturn(List.of(message));

        List<Message> messages = messageService.getMessages("room-1", null, null, 10);

        verify(messageRepository).findLatestByRoomId(eq("room-1"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).usingRecursiveComparison().isEqualTo(expectedMessage);
    }

    @Test
    void getMessages_whenCursorProvided_shouldUseBeforeQuery() {
        Instant cursorCreatedAt = Instant.parse("2025-01-02T00:00:00Z");
        MessageEntity message = new MessageEntity();
        message.setRoomId("room-1");
        message.setSender("bob");
        message.setContent("older");
        message.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
        Message chatMessage = new Message();
        chatMessage.setRoomId("room-1");
        chatMessage.setSender("bob");
        chatMessage.setContent("older");
        chatMessage.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));

        when(messageRepository.findByRoomIdBeforeCursor(eq("room-1"), eq(cursorCreatedAt), eq(123L), any(Pageable.class)))
                .thenReturn(List.of(message));

        List<Message> messages = messageService.getMessages("room-1", cursorCreatedAt, 123L, 5);

        verify(messageRepository).findByRoomIdBeforeCursor(eq("room-1"), eq(cursorCreatedAt), eq(123L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).usingRecursiveComparison().isEqualTo(chatMessage);
    }
}
