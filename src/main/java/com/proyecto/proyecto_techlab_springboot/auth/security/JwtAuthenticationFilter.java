package com.proyecto.proyecto_techlab_springboot.auth.security;

import com.proyecto.proyecto_techlab_springboot.auth.service.JwtService;
import com.proyecto.proyecto_techlab_springboot.user.model.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);
        if (!jwtService.validateTokenStructure(token)) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        try {
            UUID userId = UUID.fromString(
                    jwtService.extractClaim(token, claims -> claims.get("userId", String.class))
            );
            String email = jwtService.extractUsername(token);
            String roleClaim = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            if (email == null || roleClaim == null) {
                throw new IllegalArgumentException("El token no contiene los claims de autenticación requeridos.");
            }

            UserRole role = UserRole.valueOf(roleClaim);
            AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(userId, email, role);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority(role.name()))
                    );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (IllegalArgumentException | NullPointerException ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
