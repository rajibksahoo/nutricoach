package com.nutricoach.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.HashMap;

@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiry-hours:72}")
    private long expiryHours;

    public String generateToken(String phone, UUID coachId, String role) {
        return Jwts.builder()
                .subject(phone)
                .claims(Map.of("coachId", coachId.toString(), "role", role))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryHours * 3600 * 1000))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateClientToken(String phone, UUID clientId, UUID coachId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("clientId", clientId.toString());
        claims.put("coachId", coachId.toString());
        claims.put("role", "ROLE_CLIENT");
        return Jwts.builder()
                .subject(phone)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryHours * 3600 * 1000))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractPhone(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractCoachId(String token) {
        String coachId = extractClaim(token, claims -> claims.get("coachId", String.class));
        return UUID.fromString(coachId);
    }

    /** Returns the role claim, or "ROLE_COACH" if absent (backward-compat with old tokens). */
    public String extractRole(String token) {
        String role = extractClaim(token, claims -> claims.get("role", String.class));
        return (role != null) ? role : "ROLE_COACH";
    }

    public UUID extractClientId(String token) {
        String clientId = extractClaim(token, claims -> claims.get("clientId", String.class));
        return UUID.fromString(clientId);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String phone = extractPhone(token);
        return phone.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}