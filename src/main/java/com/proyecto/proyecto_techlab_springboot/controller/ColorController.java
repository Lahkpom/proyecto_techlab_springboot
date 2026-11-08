package com.proyecto.proyecto_techlab_springboot.controller;

import com.proyecto.proyecto_techlab_springboot.dto.color.ColorResponse;
import com.proyecto.proyecto_techlab_springboot.service.ColorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/colors")
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
    public ResponseEntity<ColorResponse> getColorById(@PathVariable Long id) {
        return ResponseEntity.ok(colorService.getColorById(id));
    }
}