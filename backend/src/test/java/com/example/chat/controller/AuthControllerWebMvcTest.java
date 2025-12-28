package com.example.chat.controller;

import com.example.chat.persistence.RefreshToken;
import com.example.chat.persistence.User;
import com.example.chat.security.JwtTokenProvider;
import com.example.chat.service.AuthService;
import com.example.chat.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ContextConfiguration(classes = com.example.chat.ChatApplication.class)
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean UserService userService;
    @MockBean PasswordEncoder passwordEncoder;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean AuthService authService;

    @Captor
    ArgumentCaptor<User> userCaptor;
    @Captor
    ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    private static User user(String username, String passwordHash) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        return user;
    }

    @Test
    void register_shouldCallService_andReturnOk() throws Exception {
        when(userService.userExists("alice")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        User expectedUser = new User("alice", "hash");


        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"pw"}
                                """))
                .andExpect(status().isOk());

        verify(userService, times(1)).save(userCaptor.capture());
        assertThat(userCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedUser);
        verify(passwordEncoder, times(1)).encode("pw");
        verifyNoMoreInteractions(authService);
    }

    @Test
    void login_shouldSetAccessAndRefreshCookies_andReturnUsername() throws Exception {
        when(userService.findByUsername("alice"))
                .thenReturn(Optional.of(user("alice", "HASH")));
        when(passwordEncoder.matches("pw", "HASH"))
                .thenReturn(true);

        when(jwtTokenProvider.createAccessToken("alice", List.of("ROLE_USER")))
                .thenReturn("ACCESS_TOKEN");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"pw"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                // Cookie matchers look at Set-Cookie headers
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(header().string("Set-Cookie", containsString("accessToken=ACCESS_TOKEN")))
                .andExpect(header().stringValues("Set-Cookie",
                        hasItem(matchesRegex(".*refreshToken=[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}.*"))
                ));

        verify(userService).findByUsername("alice");
        verify(passwordEncoder).matches("pw", "HASH");
        verify(authService).save(refreshTokenCaptor.capture());
        RefreshToken actualRefreshToken = refreshTokenCaptor.getValue();
        assertThat(actualRefreshToken.getToken()).isNotNull();
        assertThat(actualRefreshToken.getUsername()).isEqualTo("alice");
    }

    @Test
    void login_shouldReturn401_whenUserNotFound() throws Exception {
        when(userService.findByUsername("alice")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"pw"}
                                """))
                .andExpect(status().isUnauthorized());

        verify(userService).findByUsername("alice");
        verifyNoInteractions(passwordEncoder, jwtTokenProvider, authService);
    }

    @Test
    void login_shouldReturn401_whenPasswordMismatch() throws Exception {
        when(userService.findByUsername("alice"))
                .thenReturn(Optional.of(user("alice", "HASH")));
        when(passwordEncoder.matches("pw", "HASH"))
                .thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"pw"}
                                """))
                .andExpect(status().isUnauthorized());

        verify(userService).findByUsername("alice");
        verify(passwordEncoder).matches("pw", "HASH");
        verifyNoInteractions(jwtTokenProvider, authService);
    }

    @Test
    void refresh_shouldReturn401_whenNoRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(authService, jwtTokenProvider);
    }

    @Test
    void refresh_shouldReturn401_whenRefreshNotFound() throws Exception {
        when(authService.findById("RT_BAD")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "RT_BAD")))
                .andExpect(status().isUnauthorized());

        verify(authService).findById("RT_BAD");
        verifyNoMoreInteractions(authService);
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void refresh_shouldSetNewCookies_whenRefreshValid() throws Exception {
        RefreshToken existing = new RefreshToken();
        existing.setToken("RT_OLD");
        existing.setUsername("alice");

        when(authService.findById("RT_OLD"))
                .thenReturn(Optional.of(existing));

        when(jwtTokenProvider.createAccessToken("alice", List.of("ROLE_USER")))
                .thenReturn("ACCESS_2");

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "RT_OLD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(header().string("Set-Cookie", containsString("accessToken=ACCESS_2")))
                .andExpect(header().stringValues("Set-Cookie",
                        hasItem(matchesRegex(".*refreshToken=[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}.*"))
                ));

        verify(authService).deleteById("RT_OLD");
        verify(jwtTokenProvider).createAccessToken("alice", List.of("ROLE_USER"));
        verify(authService).save(refreshTokenCaptor.capture());
        RefreshToken actualRefreshToken = refreshTokenCaptor.getValue();
        assertThat(actualRefreshToken.getToken()).isNotNull();
        UUID uuid = UUID.fromString(actualRefreshToken.getToken());
        assertThat(uuid.version()).isEqualTo(4);
        assertThat(uuid.variant()).isEqualTo(2);
        assertThat(actualRefreshToken.getUsername()).isEqualTo("alice");
    }

    @Test
    void logout_shouldRevokeAndClearCookies_whenCookiePresent() throws Exception {
        doNothing().when(authService).revokeAllForToken("RT_ABC");

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("refreshToken", "RT_ABC")))
                .andExpect(status().isOk())
                // These are great because they handle multiple Set-Cookie headers automatically
                .andExpect(cookie().value("accessToken", ""))
                .andExpect(cookie().maxAge("accessToken", 0))
                .andExpect(cookie().value("refreshToken", ""))
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andExpect(result -> {
                    List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
                    assertThat(setCookies).anySatisfy(v ->
                            assertThat(v).contains("accessToken=").contains("Max-Age=0"));
                    assertThat(setCookies).anySatisfy(v ->
                            assertThat(v).contains("refreshToken=").contains("Max-Age=0"));
                });

        verify(authService).revokeAllForToken("RT_ABC");
    }
}
