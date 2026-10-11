package com.proyecto.proyecto_techlab_springboot.controller;

import com.proyecto.proyecto_techlab_springboot.dto.color.ColorCreateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.color.ColorResponse;
import com.proyecto.proyecto_techlab_springboot.dto.color.ColorUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.dto.StatusUpdateRequest;
import com.proyecto.proyecto_techlab_springboot.service.ColorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/colores")
@RequiredArgsConstructor
public class ColorController {

    private final ColorService colorService;

    @GetMapping
    public ResponseEntity<List<ColorResponse>> getAllColors(
            @RequestParam(defaultValue = "false")
            boolean includeInactive
    ) {
        return ResponseEntity.ok(colorService.getColors(includeInactive));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ColorResponse> getColorById(@PathVariable Integer id) {
        return ResponseEntity.ok(colorService.getColorById(id));
    }

    @PostMapping
//    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<ColorResponse> createColor(@Valid @RequestBody ColorCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(colorService.createColor(request));
    }

    @PutMapping("/{id}")
    //    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<ColorResponse> updateColor(
            @PathVariable       Integer             id,
            @Valid @RequestBody ColorUpdateRequest  request
    ) {
        return ResponseEntity.ok(colorService.updateColor(id, request));
    }

    @PatchMapping("/{id}/status")
    //    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<ColorResponse> updateColorStatus(
            @PathVariable       Integer             id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        return ResponseEntity.ok(colorService.updateColorStatus(id, request));
    }
}