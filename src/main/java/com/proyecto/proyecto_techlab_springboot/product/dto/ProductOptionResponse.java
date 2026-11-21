package com.proyecto.proyecto_techlab_springboot.product.dto;

import com.proyecto.proyecto_techlab_springboot.product.model.TargetGender;

import java.util.UUID;

public record ProductOptionResponse(UUID id, String name, TargetGender targetGender) {}
