package com.proyecto.proyecto_techlab_springboot.salereturn.dto;

import com.proyecto.proyecto_techlab_springboot.salereturn.model.ReturnReason;
import com.proyecto.proyecto_techlab_springboot.salereturn.model.ReturnResolution;
import com.proyecto.proyecto_techlab_springboot.salereturn.model.ReturnStatus;
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