package com.proyecto.proyecto_techlab_springboot.dto.salereturn;

import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnReason;
import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnResolution;
import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record SaleReturnSummaryResponse(
        UUID                returnId,
        UUID                saleId,
        UUID                trackingToken,
        String              customerName,
        String              customerEmail,
        ReturnReason        returnReason,
        ReturnResolution    returnResolution,
        ReturnStatus        returnStatus,
        BigDecimal          refundAmount,
        Instant             createdAt,
        Instant             closedAt
) {}