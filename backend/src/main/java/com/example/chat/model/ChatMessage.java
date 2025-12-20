package com.example.chat.model;

public class ChatMessage {

    private String roomId;
    private String sender;
    private String content;

    public ChatMessage() {}

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
