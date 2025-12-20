package com.example.chat.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    private String token;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public RefreshToken() {}

    public RefreshToken(String token, String username) {
        this.token = token;
        this.username = username;
        this.createdAt = Instant.now();
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
