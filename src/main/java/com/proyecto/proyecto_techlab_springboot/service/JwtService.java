package com.proyecto.proyecto_techlab_springboot.service;

import com.proyecto.proyecto_techlab_springboot.model.Users;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    /**
     * Genera un Access Token estándar para un usuario registrado.
     */
    public String generateAccessToken(Users user) {
        Map<String, Object> extraClaims = Map.of(
                "userId", user.getUserId().toString(),
                "role"  , user.getUserRole().name()
        );
        return buildToken(extraClaims, user.getUserEmail(), jwtExpiration);
    }

    /**
     * Extrae el email (subject) del token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae un claim genérico usando un extractor funcional.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claims != null ? claimsResolver.apply(claims) : null;
    }

    /**
     * Valida si el token le pertenece al usuario y si aún no ha expirado.
     */
    public boolean isTokenValid(String token, String userEmail) {
        final String username = extractUsername(token);
        return (username != null && username.equalsIgnoreCase(userEmail) && !isTokenExpired(token));
    }

    /**
     * Valida puramente la firma y estructura del token sin contrastar con un usuario en base.
     */
    public boolean validateTokenStructure(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);

            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("El token JWT ha expirado: {}", ex.getMessage());
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Firma o estructura del JWT inválida: {}", ex.getMessage());
        }
        return false;
    }

    private boolean isTokenExpired(String token) {
        Date expiration     = extractClaim(token, Claims::getExpiration);
        return expiration   != null && expiration.toInstant().isBefore(Instant.now());
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        Instant now             = Instant.now();
        Instant expiryInstant   = now.plusMillis(expirationMs);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryInstant))
                .signWith(getSigningKey()) // JJWT 0.12.x infiere HMAC-SHA256 a partir del tipo de clave
                .compact();
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            log.warn("Token JWT expirado al extraer claims: {}", ex.getMessage());
            return null;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Error al parsear JWT: {}", ex.getMessage());
            return null;
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secretKey)
        );
    }
}