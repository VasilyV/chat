package com.example.chat.service;

import com.example.chat.persistence.RefreshToken;
import com.example.chat.persistence.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class AuthService {

    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public void save(RefreshToken refreshToken) {
        refreshTokenRepository.save(refreshToken);
    }

    public void revokeAllForToken(String token) {
        if (token != null && !token.isBlank()) {
            refreshTokenRepository.findByToken(token).ifPresent(found -> {
                refreshTokenRepository.deleteByUsername(found.getUsername());
            });
        }
    }

    public Optional<RefreshToken> findById(String refreshToken) {
        return refreshTokenRepository.findById(refreshToken);
    }

    public void deleteById(String refreshToken) {
        refreshTokenRepository.deleteById(refreshToken);
    }
}

