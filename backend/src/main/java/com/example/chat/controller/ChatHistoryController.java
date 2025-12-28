package com.example.chat.controller;

import com.example.chat.dto.ChatMessageView;
import com.example.chat.model.ChatMessage;
import com.example.chat.service.ChatMessageService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatHistoryController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final ChatMessageService service;

    public ChatHistoryController(ChatMessageService service) {
        this.service = service;
    }

    @GetMapping("/rooms/{roomId}/messages")
    public CursorPageResponse getMessages(
            @PathVariable String roomId,
            @RequestParam(name = "limit", defaultValue = "" + DEFAULT_LIMIT) int limit,
            @RequestParam(name = "cursorCreatedAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant cursorCreatedAt,
            @RequestParam(name = "cursorId", required = false) Long cursorId
    ) {
        validateCompleteCursor(cursorCreatedAt, cursorId);

        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        List<ChatMessage> messages = service.getMessages(roomId, cursorCreatedAt, cursorId, safeLimit + 1);

        boolean hasMore = messages.size() > safeLimit;
        if (hasMore) {
            messages = messages.subList(0, safeLimit);
        }

        Cursor nextCursor = getNextCursor(hasMore, messages);

        List<ChatMessageView> content = messages.stream()
                .map(ChatMessageView::fromModel)
                .toList();

        return new CursorPageResponse(content, hasMore, nextCursor);
    }

    public record Cursor(Instant createdAt, Long id) {}

    public record CursorPageResponse(
            List<ChatMessageView> content,
            boolean hasMore,
            Cursor nextCursor
    ) {}

    private void validateCompleteCursor(Instant cursorCreatedAt, Long cursorId) {
        if ((cursorCreatedAt == null) != (cursorId == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Both cursorCreatedAt and cursorId must be provided together"
            );
        }
    }

    private Cursor getNextCursor(boolean hasMore, List<ChatMessage> messages) {
        Cursor nextCursor = null;
        if (hasMore && !messages.isEmpty()) {
            ChatMessage last = messages.getLast();
            nextCursor = new Cursor(last.getCreatedAt(), last.getId());
        }
        return nextCursor;
    }
}
