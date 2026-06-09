package com.auth.security;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth.entities.Role;
import com.auth.entities.Users;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;

@Service
@Getter
@Setter
public class JwtSecurity {

    private final SecretKey key;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final String issuer;

    public JwtSecurity(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-ttl-seconds}") long accessTtlSeconds,
            @Value("${security.jwt.refresh-ttl-seconds}") long refreshTtlSeconds,
            @Value("${security.jwt.issuer}") String issuer) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "security.jwt.secret is missing");
        }

        byte[] keyBytes;

        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "JWT secret is not valid Base64");
        }

        if (keyBytes.length < 64) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least 64 bytes for HS512. Current bytes: "
                            + keyBytes.length);
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
        this.issuer = issuer;
    }

    public String generateAccessToken(Users user) {

        Instant now = Instant.now();

        List<String> roles = user.getRole() == null
                ? List.of()
                : user.getRole()
                        .stream()
                        .map(Role::getName)
                        .toList();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .claims(Map.of(
                        "email", user.getEmail(),
                        "roles", roles,
                        "typ", "access"))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    public String generateRefreshToken(Users user, String jti) {

        Instant now = Instant.now();

        return Jwts.builder()
                .id(jti)
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
                .claim("typ", "refresh")
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    public Jws<Claims> parse(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    public boolean isAccessToken(String token) {

        Claims claims = parse(token).getPayload();

        return "access".equals(claims.get("typ"));
    }

    public boolean isRefreshToken(String token) {

        Claims claims = parse(token).getPayload();

        return "refresh".equals(claims.get("typ"));
    }

    public UUID getUserId(String token) {

        Claims claims = parse(token).getPayload();

        return UUID.fromString(claims.getSubject());
    }

    public String getEmail(String token) {

        Claims claims = parse(token).getPayload();

        return (String) claims.get("email");
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {

        Claims claims = parse(token).getPayload();

        return (List<String>) claims.get("roles");
    }

    public String getJti(String token) {

        Claims claims = parse(token).getPayload();

        return claims.getId();
    }
}