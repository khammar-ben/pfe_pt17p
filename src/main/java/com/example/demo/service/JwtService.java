package com.example.demo.service;

import com.example.demo.domain.Utilisateur;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Utilisateur utilisateur) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(utilisateur.getLogin())
                .claim("userId", utilisateur.getId())
                .claim("role", utilisateur.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(24 * 60 * 60)))
                .signWith(key)
                .compact();
    }

    public String extractLogin(String token) {
        return claims(token).getSubject();
    }

    public boolean isValid(String token, Utilisateur utilisateur) {
        Claims claims = claims(token);
        return utilisateur.getLogin().equals(claims.getSubject())
                && claims.getExpiration().after(new Date());
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
