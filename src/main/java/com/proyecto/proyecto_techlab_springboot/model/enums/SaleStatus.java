package com.proyecto.proyecto_techlab_springboot.model.enums;

public enum SaleStatus {
    PENDIENTE,
    PREPARACION,
    ENVIADO,
    ENTREGADO,
    CANCELADO,
    REEMBOLSADO,
    AUDITORIA,              // Orden retenida por discrepancias de pago, fraude potencial o revisión manual
    RECLAMO_ABIERTO,        // Disputa o solicitud de devolución en trámite
    CERRADO_CON_DEVOLUCION  // Expediente de devolución finalizado con reintegro/cambio
}