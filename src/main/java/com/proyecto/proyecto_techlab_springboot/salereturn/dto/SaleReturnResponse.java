package com.proyecto.proyecto_techlab_springboot.salereturn.dto;

import com.proyecto.proyecto_techlab_springboot.salereturn.model.ReturnReason;
import com.proyecto.proyecto_techlab_springboot.salereturn.model.ReturnResolution;
import com.proyecto.proyecto_techlab_springboot.salereturn.model.ReturnStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record SaleReturnResponse(
        UUID                returnId,
        UUID                saleId,
        UUID                trackingToken,
        ReturnReason        returnReason,
        ReturnResolution    returnResolution,
        ReturnStatus        returnStatus,
        Boolean             shippingCoveredByStore,
        String              returnShippingGuide,
        BigDecimal          refundAmount,
        String              customerNotes,
        String              inspectionNotes,
        String              inspectionEvidencePath,
        UUID                inspectedBy,
        Instant             inspectedAt,
        Instant             createdAt,
        Instant             updatedAt,
        Instant             closedAt,
        List<SaleReturnItemResponse> items
) {}