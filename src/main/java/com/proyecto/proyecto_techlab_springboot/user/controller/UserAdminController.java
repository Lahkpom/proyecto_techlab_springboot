package com.proyecto.proyecto_techlab_springboot.user.controller;

import com.proyecto.proyecto_techlab_springboot.auth.security.AuthenticatedUserPrincipal;
import com.proyecto.proyecto_techlab_springboot.common.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import com.proyecto.proyecto_techlab_springboot.user.dto.AdminUserUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.user.dto.UserAdminSegment;
import com.proyecto.proyecto_techlab_springboot.user.dto.UserResponse;
import com.proyecto.proyecto_techlab_springboot.user.model.UserRole;
import com.proyecto.proyecto_techlab_springboot.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('OPERATOR', 'ADMIN')")
public class UserAdminController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @AuthenticationPrincipal AuthenticatedUserPrincipal actor,
            @RequestParam(defaultValue = "CLIENTS") UserAdminSegment segment,
            @RequestParam(required = false) UserRole roleFilter,
            @RequestParam(required = false) RowStatus rowStatus,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(userService.searchUsersAdmin(
                actor,
                segment,
                roleFilter,
                rowStatus,
                search,
                pageable
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUserUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal actor
    ) {
        return ResponseEntity.ok(userService.updateAdminUser(id, request, actor));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal actor
    ) {
        return ResponseEntity.ok(userService.updateAdminUserStatus(id, request, actor));
    }
}
