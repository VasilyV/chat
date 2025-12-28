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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    private static User user(String username, String passwordHash) {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(passwordHash);
        return u;
    }

    @Test
    void register_shouldCallService_andReturnOk() throws Exception {
        when(userService.userExists("alice")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        User expectedUser = new User("alice", "pw");


        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"pw"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verify(userService, times(1)).save(userCaptor.capture());
        assertThat(userCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedUser);
        verify(passwordEncoder, times(1)).encode("");
        verifyNoMoreInteractions(authService);
    }

    @Test
    void login_shouldSetAccessAndRefreshCookies_andReturnUsername() throws Exception {
        when(userService.findByUsername("alice"))
                .thenReturn(Optional.of(user("alice", "HASH")));
        when(passwordEncoder.matches("pw", "HASH"))
                .thenReturn(true);

        when(jwtTokenProvider.generateAccessToken("alice"))
                .thenReturn("ACCESS_1");
        when(authService.createRefreshToken("alice"))
                .thenReturn("REFRESH_1");

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
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("accessToken=ACCESS_1")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refreshToken=REFRESH_1")));

        verify(userService).findByUsername("alice");
        verify(passwordEncoder).matches("pw", "HASH");
        verify(jwtTokenProvider).generateAccessToken("alice");
        verify(authService).createRefreshToken("alice");
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
        when(authService.findRefreshToken("RT_BAD")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "RT_BAD")))
                .andExpect(status().isUnauthorized());

        verify(authService).findRefreshToken("RT_BAD");
        verifyNoMoreInteractions(authService);
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void refresh_shouldSetNewCookies_whenRefreshValid() throws Exception {
        RefreshToken existing = new RefreshToken();
        existing.setToken("RT_OLD");
        existing.setUsername("alice");

        when(authService.findRefreshToken("RT_OLD"))
                .thenReturn(Optional.of(existing));

        when(jwtTokenProvider.generateAccessToken("alice"))
                .thenReturn("ACCESS_2");
        when(authService.createRefreshToken("alice"))
                .thenReturn("REFRESH_2");

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "RT_OLD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("accessToken=ACCESS_2")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refreshToken=REFRESH_2")));

        verify(authService).findRefreshToken("RT_OLD");
        verify(jwtTokenProvider).generateAccessToken("alice");
        verify(authService).createRefreshToken("alice");
    }

    @Test
    void logout_shouldRevokeAndClearCookies_whenCookiePresent() throws Exception {
        doNothing().when(authService).revokeAllForToken("RT_ABC");

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("refreshToken", "RT_ABC")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                // your controller clears cookies via Max-Age=0
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("accessToken=;")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refreshToken=;")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

        verify(authService).revokeAllForToken("RT_ABC");
    }
}
