package com.example.chat.service;

import com.example.chat.persistence.ChatMessageEntity;
import com.example.chat.persistence.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatMessageService {

    private final ChatMessageRepository repo;

    public ChatMessageService(ChatMessageRepository repo) {
        this.repo = repo;
    }

    public ChatMessageEntity save(String roomId, String sender, String content) {
        ChatMessageEntity msg = new ChatMessageEntity();
        msg.setRoomId(roomId);
        msg.setSender(sender);
        msg.setContent(content);
        return repo.save(msg);
    }

    public List<ChatMessageEntity> latestMessages(String roomId) {
        return repo.findTop50ByRoomIdOrderBySentAtDesc(roomId);
    }
}
