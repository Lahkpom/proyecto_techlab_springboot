package com.proyecto.proyecto_techlab_springboot.dto.fiscal;

import com.proyecto.proyecto_techlab_springboot.model.enums.FiscalReceiptType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record FiscalReceiptResponse(
        UUID                fiscalReceiptId,
        UUID                saleId,
        UUID                returnId,
        FiscalReceiptType   receiptType,
        Integer             pointOfSale,
        Long                receiptNumber,
        String              formattedReceiptNumber, // Ej: "00001-00000123"
        String              cae,
        LocalDate           caeDueDate,
        LocalDate           issueDate,
        Integer             docType,
        String              docNumber,
        String              customerName,
        BigDecimal          totalAmount,
        String              qrPayload,
        Instant             createdAt
) {}