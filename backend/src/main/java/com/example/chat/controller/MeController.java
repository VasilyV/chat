package com.example.chat.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class MeController {

    private static final Logger log = LoggerFactory.getLogger(MeController.class);

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Not logged in");
        }
        log.debug("/me requested principal={}", auth.getName() == null ? "null" : auth.getName());
        return ResponseEntity.ok(Map.of("username", auth.getName()));
    }
}
