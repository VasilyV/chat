package com.example.chat.auth;

import com.example.chat.controller.MeController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MeControllerTest {

    private final MeController controller = new MeController();

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void me_shouldReturn401_whenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        ResponseEntity<?> res = controller.me(new MockHttpServletRequest());

        assertThat(res.getStatusCode().value()).isEqualTo(401);
        assertThat(res.getBody()).isEqualTo("Not logged in");
    }

    @Test
    @SuppressWarnings("unchecked")
    void me_shouldReturnUsername_whenAuthenticated() {
        var auth = new UsernamePasswordAuthenticationToken("alice", null, List.of(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                return "";
            }
        }));
        SecurityContextHolder.getContext().setAuthentication(auth);

        ResponseEntity<?> res = controller.me(new MockHttpServletRequest());

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<String, String> body = (Map<String, String>) res.getBody();
        assertThat(body).containsEntry("username", "alice");
    }
}
