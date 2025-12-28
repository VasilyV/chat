package com.example.chat.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {
    @Query("""
        select m from ChatMessageEntity m
        where m.roomId = :roomId
        order by m.createdAt desc, m.id desc
    """)
    List<ChatMessageEntity> findLatestByRoomId(
            @Param("roomId") String roomId,
            Pageable pageable
    );

    @Query("""
        select m from ChatMessageEntity m
        where m.roomId = :roomId
          and (
                m.createdAt < :cursorCreatedAt
             or (m.createdAt = :cursorCreatedAt and m.id < :cursorId)
          )
        order by m.createdAt desc, m.id desc
    """)
    List<ChatMessageEntity> findByRoomIdBeforeCursor(
            @Param("roomId") String roomId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
