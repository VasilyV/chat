package com.example.chat.auth;

import com.example.chat.persistence.UserRepository;
import com.example.chat.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public record LoginRequest(String username, String password) {}
    public record TokenResponse(String accessToken, String refreshToken) {}

    public AuthController(JwtTokenProvider jwtTokenProvider, RefreshTokenRepository refreshTokenRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest req) {
        if (req.username == null || req.username.isBlank() || req.password == null || req.password.isBlank()) {
            return ResponseEntity.badRequest().body("Missing username or password");
        }

        if (userRepository.existsById(req.username)) {
            return ResponseEntity.badRequest().body("User already exists");
        }

        String hash = passwordEncoder.encode(req.password);
        userRepository.save(new User(req.username, hash));

        return ResponseEntity.ok("Registered");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletResponse response) {
        var user = userRepository.findById(req.username);
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        if (!passwordEncoder.matches(req.password, user.get().getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String access = jwtTokenProvider.createAccessToken(req.username, List.of("ROLE_USER"));
        String refresh = UUID.randomUUID().toString();
        refreshTokenRepository.save(new RefreshToken(refresh, req.username));

        ResponseCookie cookie = ResponseCookie.from("accessToken", access)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("None")
                .maxAge(60 * 60 * 24 * 30)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok().body(Map.of("refreshToken", refresh));
    }



    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refresh = body.get("refreshToken");
        if (refresh == null) return ResponseEntity.status(401).build();

        return refreshTokenRepository.findById(refresh)
                .map(rt -> {
                    String access = jwtTokenProvider.createAccessToken(rt.getUsername(), List.of("ROLE_USER"));
                    return ResponseEntity.ok(Map.of("accessToken", access));
                })
                .orElse(ResponseEntity.status(401).build());
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> revoke(@RequestBody Map<String, String> body) {
        String refresh = body.get("refreshToken");
        if (refresh != null) {
            refreshTokenRepository.deleteById(refresh);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("accessToken", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        return ResponseEntity.ok().build();
    }
}
