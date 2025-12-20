package com.example.chat.controller;

import com.example.chat.persistence.ChatMessageEntity;
import com.example.chat.service.ChatMessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatHistoryController {

    private final ChatMessageService service;

    public ChatHistoryController(ChatMessageService service) {
        this.service = service;
    }

    @GetMapping("/rooms/{roomId}/messages")
    public List<ChatMessageEntity> history(@PathVariable("roomId") String roomId) {
        System.out.println("Reached controller");
        return service.latestMessages(roomId);
    }
}
