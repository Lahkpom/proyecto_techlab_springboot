package com.proyecto.proyecto_techlab_springboot.dto.salereturn;

import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnReason;
import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnResolution;
import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnStatus;
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