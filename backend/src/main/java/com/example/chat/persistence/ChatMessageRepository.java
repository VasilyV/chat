package com.example.chat.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {
    List<ChatMessageEntity> findTop50ByRoomIdOrderBySentAtDesc(String roomId);
}
