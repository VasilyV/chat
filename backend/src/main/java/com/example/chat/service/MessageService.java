package com.example.chat.service;

import com.example.chat.model.Message;
import com.example.chat.persistence.MessageEntity;
import com.example.chat.persistence.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void save(String roomId, String sender, String content) {
        log.debug("Persisting message roomId={} sender={}", roomId, sender);
        MessageEntity message = new MessageEntity();
        message.setRoomId(roomId);
        message.setSender(sender);
        message.setContent(content);
        messageRepository.save(message);
    }

    public List<Message> getMessages(
            String roomId,
            Instant cursorCreatedAt,
            Long cursorId,
            int limit
    ) {
        log.debug("Fetching messages roomId={} limit={} cursorCreatedAt={} cursorId={}", roomId, limit, cursorCreatedAt, cursorId);
        var pageable = PageRequest.of(0, limit);

        if (cursorCreatedAt == null || cursorId == null) {
            return messageRepository.findLatestByRoomId(roomId, pageable).stream().map(this::toModel).toList();
        }

        return messageRepository.findByRoomIdBeforeCursor(
                roomId,
                cursorCreatedAt,
                cursorId,
                pageable
        ).stream().map(this::toModel).toList();
    }

    private Message toModel(MessageEntity e) {
        Message m = new Message();
        m.setId(e.getId());
        m.setRoomId(e.getRoomId());
        m.setSender(e.getSender());
        m.setContent(e.getContent());
        m.setCreatedAt(e.getCreatedAt());
        return m;
    }
}
