package com.example.chat.controller;

import com.example.chat.config.CookieProperties;
import com.example.chat.service.AuthService;
import com.example.chat.persistence.RefreshToken;
import com.example.chat.persistence.User;
import com.example.chat.service.UserService;
import com.example.chat.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final CookieProperties cookieProperties;

    public record LoginRequest(String username, String password) {}

    public AuthController(JwtTokenProvider jwtTokenProvider, AuthService authService, UserService userService, PasswordEncoder passwordEncoder, CookieProperties cookieProperties) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authService = authService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.cookieProperties = cookieProperties;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest loginRequest) {
        if (loginRequest.username == null || loginRequest.username.isBlank() || loginRequest.password == null || loginRequest.password.isBlank()) {
            return ResponseEntity.badRequest().body("Missing username or password");
        }
        log.info("Register attempt username={}", loginRequest.username);

        if (userService.userExists(loginRequest.username)) {
            return ResponseEntity.badRequest().body("User already exists");
        }

        String hash = passwordEncoder.encode(loginRequest.password);
        userService.save(new User(loginRequest.username, hash));

        return ResponseEntity.ok("Registered");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletResponse response) {
        log.info("Login attempt username={}", req.username);
        var user = userService.findByUsername(req.username);
        if (user.isEmpty()) {
            log.warn("Login failed username={}", req.username);
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        if (!passwordEncoder.matches(req.password, user.get().getPasswordHash())) {
            log.warn("Login failed username={}", req.username);
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String access = jwtTokenProvider.createAccessToken(req.username, List.of("ROLE_USER"));
        String refreshToken = UUID.randomUUID().toString();
        authService.save(new RefreshToken(refreshToken, req.username));

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", access)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .path("/")
                .sameSite(cookieProperties.getSameSite())
                .maxAge(60 * 60 * 24 * 30)
                .build();
        log.info("Login success username={}", req.username);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie(refreshToken).toString())
                .body(Map.of("username", user.get().getUsername()));

    }



    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        log.debug("Refresh attempt (cookie present={})", refreshToken != null && !refreshToken.isBlank());
        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("Refresh denied (missing/invalid refresh token cookie)");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        RefreshToken storedRefreshToken = authService.findById(refreshToken).orElse(null);
        if (storedRefreshToken == null) {
            log.warn("Refresh denied (missing/invalid refresh token cookie)");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                    .build();
        }

        String username = storedRefreshToken.getUsername();

        authService.deleteById(refreshToken);
        String newRefreshToken = UUID.randomUUID().toString();
        authService.save(new RefreshToken(newRefreshToken, username));

        String newAccess = jwtTokenProvider.createAccessToken(username, List.of("ROLE_USER"));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie(newAccess).toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie(newRefreshToken).toString())
                .body(Map.of("ok", true));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        log.info("Logout called (cookie present={})", refreshToken != null && !refreshToken.isBlank());
        authService.revokeAllForToken(refreshToken);
        log.info("Logout revoked tokens for refreshToken prefix={}",
                refreshToken == null ? "null" : refreshToken.substring(0, Math.min(8, refreshToken.length())));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccessCookie().toString())
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .body(Map.of("message", "Logged out"));
    }

    private ResponseCookie accessCookie(String token) {
        return ResponseCookie.from("accessToken", token)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path("/")
                .maxAge(Duration.ofHours(1))
                .build();
    }

    private ResponseCookie refreshCookie(String token) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path("/api/auth")
                .maxAge(Duration.ofDays(30))
                .build();
    }

    private ResponseCookie clearAccessCookie() {
        return ResponseCookie.from("accessToken", "")
                .httpOnly(true).secure(true).sameSite(cookieProperties.getSameSite())
                .path("/").maxAge(0).build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path("/api/auth")
                .maxAge(0)
                .build();
    }

}
