package com.example.chat.service;

import com.example.chat.model.ChatMessage;
import com.example.chat.persistence.ChatMessageEntity;
import com.example.chat.persistence.ChatMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessageService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    public void save(String roomId, String sender, String content) {
        ChatMessageEntity message = new ChatMessageEntity();
        message.setRoomId(roomId);
        message.setSender(sender);
        message.setContent(content);
        chatMessageRepository.save(message);
    }

    public List<ChatMessage> getMessages(
            String roomId,
            Instant cursorCreatedAt,
            Long cursorId,
            int limit
    ) {
        var pageable = PageRequest.of(0, limit); // LIMIT only; NO OFFSET

        if (cursorCreatedAt == null || cursorId == null) {
            return chatMessageRepository.findLatestByRoomId(roomId, pageable).stream().map(this::toModel).toList();
        }

        return chatMessageRepository.findByRoomIdBeforeCursor(
                roomId,
                cursorCreatedAt,
                cursorId,
                pageable
        ).stream().map(this::toModel).toList();
    }

    private ChatMessage toModel(ChatMessageEntity e) {
        ChatMessage m = new ChatMessage();
        m.setId(e.getId());
        m.setRoomId(e.getRoomId());
        m.setSender(e.getSender());
        m.setContent(e.getContent());
        m.setCreatedAt(e.getCreatedAt());
        return m;
    }
}
