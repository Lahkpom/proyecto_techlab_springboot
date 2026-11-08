package com.proyecto.proyecto_techlab_springboot.controller;

import com.proyecto.proyecto_techlab_springboot.dto.color.ColorCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.color.ColorResponse;
import com.proyecto.proyecto_techlab_springboot.dto.color.ColorUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.common.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.service.ColorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/colors")
@RequiredArgsConstructor
// @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')") // TODO: Descomentar al habilitar Spring Security
public class ColorAdminController {

    private final ColorService colorService;

    @PostMapping
    public ResponseEntity<ColorResponse> createColor(@Valid @RequestBody ColorCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(colorService.createColor(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ColorResponse> updateColor(
            @PathVariable       Long                id,
            @Valid @RequestBody ColorUpdateRequest  request
    ) {
        return ResponseEntity.ok(colorService.updateColor(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ColorResponse> updateColorStatus(
            @PathVariable       Long                id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        return ResponseEntity.ok(colorService.updateColorStatus(id, request));
    }
}