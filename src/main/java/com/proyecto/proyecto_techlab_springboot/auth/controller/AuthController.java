package com.proyecto.proyecto_techlab_springboot.auth.controller;

import com.proyecto.proyecto_techlab_springboot.auth.dto.*;
import com.proyecto.proyecto_techlab_springboot.auth.security.AuthenticatedUserPrincipal;
import com.proyecto.proyecto_techlab_springboot.user.dto.UserResponse;
import com.proyecto.proyecto_techlab_springboot.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PreAuthorize("isAnonymous()")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(authService.login(request, httpRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(authService.refreshToken(request, httpRequest));
    }

    @PreAuthorize("isAnonymous()")
    @PostMapping("/register")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.authService.createUser(request));
    }

    @PostMapping("/register/wholesalers")
    @PreAuthorize("hasAnyAuthority('OPERATOR', 'ADMIN')")
    public ResponseEntity<UserResponse> createWholesaler(@Valid @RequestBody WholesalerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createWholesaler(request));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getAuthenticatedUserProfile(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(authService.getAuthenticatedUserProfile(principal.userId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null) authService.logout(request.refreshToken());

        return ResponseEntity.noContent().build();
    }
}