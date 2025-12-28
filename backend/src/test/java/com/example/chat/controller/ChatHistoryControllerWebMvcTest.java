package com.example.chat.controller;

import com.example.chat.model.ChatMessage;
import com.example.chat.service.ChatMessageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ContextConfiguration(classes = com.example.chat.ChatApplication.class)
@WebMvcTest(ChatHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatHistoryControllerWebMvcTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ChatMessageService service;

    private static ChatMessage chatMessage(long id, Instant createdAt, String roomId) {
        ChatMessage msg = new ChatMessage();
        msg.setId(id);
        msg.setCreatedAt(createdAt);
        msg.setRoomId(roomId);
        return msg;
    }

    @Test
    void getMessages_shouldRejectHalfCursor() throws Exception {
        Instant t = Instant.parse("2025-01-01T00:00:00Z");

        mockMvc.perform(get("/api/chat/rooms/{roomId}/messages", "room")
                        .param("limit", "10")
                        .param("cursorCreatedAt", t.toString()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/chat/rooms/{roomId}/messages", "room")
                        .param("limit", "10")
                        .param("cursorId", "1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void getMessages_shouldCutLimitAndFetchLimitPlusOne() throws Exception {
        when(service.getMessages(anyString(), any(), any(), anyInt()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/chat/rooms/{roomId}/messages", "room")
                        .param("limit", "999"))
                .andExpect(status().isOk());

        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(service).getMessages(eq("room"), isNull(), isNull(), limitCaptor.capture());
        assertThat(limitCaptor.getValue()).isEqualTo(201);
    }

    @Test
    void getMessages_shouldReturnNoCursor_whenNoMoreResults() throws Exception {
        Instant t2 = Instant.parse("2025-01-02T00:00:00Z");
        Instant t1 = Instant.parse("2025-01-01T00:00:00Z");

        when(service.getMessages(eq("room"), isNull(), isNull(), eq(3)))
                .thenReturn(List.of(chatMessage(2, t2, "room"), chatMessage(1, t1, "room")));

        mockMvc.perform(get("/api/chat/rooms/{roomId}/messages", "room")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMore").value(false))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.nextCursor").value(nullValue()));
    }

    @Test
    void getMessages_shouldReturnAccordingToLimitAndHasMoreTrue() throws Exception {
        Instant t2 = Instant.parse("2025-01-02T00:00:00Z");
        Instant t1 = Instant.parse("2025-01-01T00:00:00Z");
        Instant t3 = Instant.parse("2025-01-03T00:00:00Z");

        when(service.getMessages(eq("room"), isNull(), isNull(), eq(3)))
                .thenReturn(List.of(chatMessage(2, t2, "room"), chatMessage(1, t1, "room"), chatMessage(1, t3, "room")));

        mockMvc.perform(get("/api/chat/rooms/{roomId}/messages", "room")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.nextCursor.createdAt").value("2025-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.nextCursor.id").value(1));
    }
}
