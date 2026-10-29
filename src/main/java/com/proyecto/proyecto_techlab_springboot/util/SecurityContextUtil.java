package com.proyecto.proyecto_techlab_springboot.util;

import com.proyecto.proyecto_techlab_springboot.exception.common.InvalidCredentialsException;
import com.proyecto.proyecto_techlab_springboot.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityContextUtil {

    private final JwtService jwtService;

    /**
     * Resuelve el userId si el token viene presente y es válido; si no viene, retorna null (invitado).
     */
    public UUID resolveOptionalUserIdFromToken(HttpServletRequest request) {
        String token = extractBearerToken(request);
        if (token == null || !jwtService.validateTokenStructure(token)) return null;

        String userIdClaim = jwtService.extractClaim(token, claims -> claims.get("userId", String.class));
        return userIdClaim != null
            ? UUID.fromString(userIdClaim) 
            : null;
    }

    /**
     * Exige autenticación obligatoria. Si falta el token, es inválido o no tiene claim, lanza excepción.
     */
    public UUID resolveRequiredUserIdFromToken(HttpServletRequest request) {
        String token = extractBearerToken(request);
        if (token == null)
            throw new InvalidCredentialsException("Encabezado de autorización ausente o con formato inválido.");

        if (!jwtService.validateTokenStructure(token))
            throw new InvalidCredentialsException("El token provisto es inválido o se encuentra expirado.");

        String userIdClaim = jwtService.extractClaim(token, claims -> claims.get("userId", String.class));
        if (userIdClaim == null)
            throw new InvalidCredentialsException("El token no contiene el claim de usuario requerido.");

        return UUID.fromString(userIdClaim);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        
        return authHeader.substring(7);
    }
}