package com.example.chat.controller;

import com.example.chat.dto.MessageView;
import com.example.chat.model.Message;
import com.example.chat.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class HistoryController {

    private static final Logger log = LoggerFactory.getLogger(HistoryController.class);

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final MessageService service;

    public HistoryController(MessageService service) {
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
        log.debug("Get history roomId={} limit={} cursorCreatedAt={} cursorId={}", roomId, limit, cursorCreatedAt, cursorId);

        validateCompleteCursor(cursorCreatedAt, cursorId);

        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        List<Message> messages = service.getMessages(roomId, cursorCreatedAt, cursorId, safeLimit + 1);

        boolean hasMore = messages.size() > safeLimit;
        if (hasMore) {
            messages = messages.subList(0, safeLimit);
        }

        Cursor nextCursor = getNextCursor(hasMore, messages);

        List<MessageView> content = messages.stream()
                .map(MessageView::fromModel)
                .toList();

        return new CursorPageResponse(content, hasMore, nextCursor);
    }

    public record Cursor(Instant createdAt, Long id) {}

    public record CursorPageResponse(
            List<MessageView> content,
            boolean hasMore,
            Cursor nextCursor
    ) {}

    private void validateCompleteCursor(Instant cursorCreatedAt, Long cursorId) {
        if ((cursorCreatedAt == null) != (cursorId == null)) {
            String errorMessage = "Both cursorCreatedAt and cursorId must be provided together";
            log.warn("{}: cursorCreatedAt={}, cursorId={}", errorMessage, cursorCreatedAt, cursorId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }
    }

    private Cursor getNextCursor(boolean hasMore, List<Message> messages) {
        Cursor nextCursor = null;
        if (hasMore && !messages.isEmpty()) {
            Message last = messages.getLast();
            nextCursor = new Cursor(last.getCreatedAt(), last.getId());
        }
        return nextCursor;
    }
}
