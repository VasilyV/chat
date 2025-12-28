package com.example.chat.service;

import com.example.chat.persistence.RefreshToken;
import com.example.chat.persistence.RefreshTokenRepository;
import com.example.chat.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock RefreshTokenRepository refreshTokenRepository;

    @InjectMocks AuthService authService;

    @Test
    void findById_shouldReturnRefreshToken() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("RT");
        refreshToken.setUsername("alice");
        RefreshToken expectedRefreshToken = new RefreshToken();
        expectedRefreshToken.setToken("RT");
        expectedRefreshToken.setUsername("alice");

        when(refreshTokenRepository.findById(any())).thenReturn(Optional.of(refreshToken));

        Optional<RefreshToken> result = authService.findById("RT");

        verify(refreshTokenRepository).findById("RT");
        assertThat(result).isPresent();
        assertThat(result.get()).usingRecursiveComparison().ignoringFields("createdAt").isEqualTo(expectedRefreshToken);
    }

    @Test
    void deleteById_shouldDelegateToRepo() {
        authService.deleteById("RT");

        verify(refreshTokenRepository).deleteById("RT");
    }

    @Test
    void revokeAllForToken_shouldDeleteByUsername_whenTokenFound() {
        RefreshToken rt = new RefreshToken();
        rt.setToken("RT");
        rt.setUsername("alice");

        when(refreshTokenRepository.findByToken("RT")).thenReturn(Optional.of(rt));

        authService.revokeAllForToken("RT");

        verify(refreshTokenRepository).findByToken("RT");
        verify(refreshTokenRepository).deleteByUsername("alice");
    }

    @Test
    void revokeAllForToken_doesNothing_whenTokenIsBlank() {
        authService.revokeAllForToken("");

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void revokeAllForToken_doesNothing_whenTokenIsNull() {
        authService.revokeAllForToken(null);

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void revokeAllForToken_shouldDoNothing_whenTokenMissing() {
        when(refreshTokenRepository.findByToken("RT")).thenReturn(Optional.empty());

        authService.revokeAllForToken("RT");

        verify(refreshTokenRepository).findByToken("RT");
        verify(refreshTokenRepository, never()).deleteByUsername(anyString());
    }
}
