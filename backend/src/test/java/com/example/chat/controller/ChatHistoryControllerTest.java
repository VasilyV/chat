package com.example.chat.controller;

import com.example.chat.dto.ChatMessageView;
import com.example.chat.model.ChatMessage;
import com.example.chat.service.ChatMessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatHistoryControllerTest {

    @Mock
    ChatMessageService service;

    @InjectMocks
    ChatHistoryController controller;

    @Captor
    ArgumentCaptor<Integer> limitCaptor;

    private static ChatMessage chatMessage(long id, Instant createdAt, String roomId) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(id);
        chatMessage.setRoomId(roomId);
        chatMessage.setSender("u" + id);
        chatMessage.setContent("c" + id);
        chatMessage.setCreatedAt(createdAt);
        return chatMessage;
    }

    @Test
    void getMessages_shouldRejectHalfCursor() {
        Instant t = Instant.parse("2025-01-01T00:00:00Z");

        assertThatThrownBy(() -> controller.getMessages("room", 10, t, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));

        assertThatThrownBy(() -> controller.getMessages("room", 10, null, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void getMessages_shouldClampLimitAndFetchLimitPlusOne() {
        when(service.getMessages(anyString(), any(), any(), anyInt()))
                .thenReturn(List.of());

        controller.getMessages("room", 999, null, null);

        verify(service).getMessages(eq("room"), isNull(), isNull(), limitCaptor.capture());
        assertThat(limitCaptor.getValue()).isEqualTo(201); // MAX_LIMIT (200) + 1
    }

    @Test
    void getMessages_shouldReturnHasMoreAndNextCursor_whenMoreResultsExist() {
        Instant t3 = Instant.parse("2025-01-03T00:00:00Z");
        Instant t2 = Instant.parse("2025-01-02T00:00:00Z");
        Instant t1 = Instant.parse("2025-01-01T00:00:00Z");

        // service called with safeLimit+1; we return 3 items so that safeLimit=2 hasMore=true
        when(service.getMessages(eq("room"), isNull(), isNull(), eq(3)))
                .thenReturn(List.of(
                        chatMessage(3, t3, "room"),
                        chatMessage(2, t2, "room"),
                        chatMessage(1, t1, "room")
                ));

        ChatHistoryController.CursorPageResponse res = controller.getMessages("room", 2, null, null);

        assertThat(res.hasMore()).isTrue();
        assertThat(res.content()).hasSize(2);
        assertThat(res.nextCursor()).isNotNull();
        assertThat(res.nextCursor().createdAt()).isEqualTo(t2);
        assertThat(res.nextCursor().id()).isEqualTo(2L);

        // DTO mapping
        ChatMessageView first = res.content().get(0);
        assertThat(first.roomId()).isEqualTo("room");
        assertThat(first.sender()).isEqualTo("u3");
        assertThat(first.content()).isEqualTo("c3");
        assertThat(first.createdAt()).isEqualTo(t3);
    }

    @Test
    void getMessages_shouldReturnNoCursor_whenNoMoreResults() {
        Instant t2 = Instant.parse("2025-01-02T00:00:00Z");
        Instant t1 = Instant.parse("2025-01-01T00:00:00Z");

        when(service.getMessages(eq("room"), isNull(), isNull(), eq(3)))
                .thenReturn(List.of(
                        chatMessage(2, t2, "room"),
                        chatMessage(1, t1, "room")
                ));

        ChatHistoryController.CursorPageResponse res = controller.getMessages("room", 2, null, null);

        assertThat(res.hasMore()).isFalse();
        assertThat(res.content()).hasSize(2);
        assertThat(res.nextCursor()).isNull();
    }
}
