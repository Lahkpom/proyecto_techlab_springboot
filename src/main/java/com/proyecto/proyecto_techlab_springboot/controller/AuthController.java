package com.proyecto.proyecto_techlab_springboot.controller;

import com.proyecto.proyecto_techlab_springboot.dto.auth.AuthResponse;
import com.proyecto.proyecto_techlab_springboot.dto.auth.LoginRequest;
import com.proyecto.proyecto_techlab_springboot.dto.auth.UserCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.auth.WholesalerCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.user.UserResponse;
import com.proyecto.proyecto_techlab_springboot.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.authService.createUser(request));
    }

    @PostMapping("/register/wholesalers")
    // TODO Descomentar cuando active Spring Security:
    // @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<UserResponse> createWholesaler(@Valid @RequestBody WholesalerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createWholesaler(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }


}
