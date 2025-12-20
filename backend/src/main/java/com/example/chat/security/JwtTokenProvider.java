package com.example.chat.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private final Key key;
    private final long validityMillis;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String base64Secret,
                            @Value("${app.jwt.validity-ms:3600000}") long validityMillis) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Secret));
        this.validityMillis = validityMillis;
    }

    public String createAccessToken(String username, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(validityMillis)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
    }
}
