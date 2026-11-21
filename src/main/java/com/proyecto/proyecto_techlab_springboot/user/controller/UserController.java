package com.proyecto.proyecto_techlab_springboot.user.controller;

import com.proyecto.proyecto_techlab_springboot.auth.security.AuthenticatedUserPrincipal;
import com.proyecto.proyecto_techlab_springboot.common.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.user.service.UserService;
import com.proyecto.proyecto_techlab_springboot.user.dto.PasswordUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.user.dto.UserResponse;
import com.proyecto.proyecto_techlab_springboot.user.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("#id == authentication.principal.userId() or hasAnyAuthority('OPERATOR', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(this.userService.getUserById(id));
    }

    @PreAuthorize("#id == authentication.principal.userId() or hasAnyAuthority('OPERATOR', 'ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable       UUID                id,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ResponseEntity.ok(this.userService.updateUser(id, request));
    }

    @PreAuthorize("#id == authentication.principal.userId() or hasAnyAuthority('OPERATOR', 'ADMIN')")
    @PatchMapping("/{id}/password")
    public ResponseEntity<UserResponse> updateUserPassword(
            @PathVariable       UUID                    id,
            @Valid @RequestBody PasswordUpdateRequest request
    ) {
        return ResponseEntity.ok(this.userService.updateUserPassword(id, request));
    }

    @PreAuthorize("hasAnyAuthority('OPERATOR', 'ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable       UUID                id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal actor
    ) {
        return ResponseEntity.ok(this.userService.updateUserStatus(id, request, actor));
    }
}
