package com.example.chat.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class MeController {

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Not logged in");
        }
        return ResponseEntity.ok(Map.of("username", auth.getName()));
    }
}
