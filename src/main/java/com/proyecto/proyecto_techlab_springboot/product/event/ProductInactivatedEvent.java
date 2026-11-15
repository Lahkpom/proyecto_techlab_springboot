package com.proyecto.proyecto_techlab_springboot.product.event;

import java.util.UUID;

public record ProductInactivatedEvent(UUID productId) {}