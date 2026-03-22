package com.example.chat.dto;

import com.example.chat.model.Message;

import java.time.Instant;

public record MessageView(
        String roomId,
        String sender,
        String content,
        Instant createdAt
) {
    public static MessageView fromModel(Message e) {
        return new MessageView(
                e.getRoomId(),
                e.getSender(),
                e.getContent(),
                e.getCreatedAt()
        );
    }
}

