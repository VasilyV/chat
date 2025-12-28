package com.example.chat.dto;

import com.example.chat.model.ChatMessage;

import java.time.Instant;

public record ChatMessageView(
        String roomId,
        String sender,
        String content,
        Instant createdAt
) {
    public static ChatMessageView fromModel(ChatMessage e) {
        return new ChatMessageView(
                e.getRoomId(),
                e.getSender(),
                e.getContent(),
                e.getCreatedAt()
        );
    }
}

