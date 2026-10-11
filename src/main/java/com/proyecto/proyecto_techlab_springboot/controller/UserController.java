package com.proyecto.proyecto_techlab_springboot.controller;

import com.proyecto.proyecto_techlab_springboot.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.user.*;
import com.proyecto.proyecto_techlab_springboot.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    // Algún tipo de autorización para que los usuarios solo puedan pedir su propia información y no la de otros
    public ResponseEntity<UserResponse> getColorById(@PathVariable UUID id) {
        return ResponseEntity.ok(this.userService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.createUser(request));
    }

    @PostMapping("/wholesalers")
//    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<UserResponse> createWholesaler(@Valid @RequestBody WholesalerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.createWholesaler(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable       UUID                id,
            @Valid @RequestBody UserUpdateRequest   request
    ) {
        return ResponseEntity.ok(this.userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/email")
    public ResponseEntity<UserResponse> updateUserEmail(
            @PathVariable       UUID                id,
            @Valid @RequestBody EmailUpdateRequest  request
    ) {
        return ResponseEntity.ok(this.userService.updateUserEmail(id, request));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<UserResponse> updateUserPassword(
            @PathVariable       UUID                    id,
            @Valid @RequestBody PasswordUpdateRequest   request
    ) {
        return ResponseEntity.ok(this.userService.updateUserPassword(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable       UUID                id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        return ResponseEntity.ok(this.userService.updateUserStatus(id, request));
    }


}
