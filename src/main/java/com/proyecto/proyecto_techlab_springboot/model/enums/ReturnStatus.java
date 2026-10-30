package com.proyecto.proyecto_techlab_springboot.model.enums;

public enum ReturnStatus {
    SOLICITADA,             // Reclamo iniciado por el cliente
    EN_TRANSITO,            // Paquete despachado hacia el depósito con guía inversa
    RECIBIDO_EN_CONTROL,    // Paquete recepcionado en taller para inspección
    APROBADA,               // Control de calidad validó el estado de las prendas
    RECHAZADA,              // Control constató uso, daño no imputable o arrepentimiento improcedente
    CANCELADA               // Cliente desistió de la devolución
}