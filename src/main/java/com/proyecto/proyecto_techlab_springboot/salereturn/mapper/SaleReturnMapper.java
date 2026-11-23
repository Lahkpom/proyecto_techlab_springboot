package com.proyecto.proyecto_techlab_springboot.salereturn.mapper;

import com.proyecto.proyecto_techlab_springboot.sale.model.SaleItem;
import com.proyecto.proyecto_techlab_springboot.salereturn.dto.SaleReturnItemResponse;
import com.proyecto.proyecto_techlab_springboot.salereturn.dto.SaleReturnResponse;
import com.proyecto.proyecto_techlab_springboot.salereturn.dto.SaleReturnSummaryResponse;
import com.proyecto.proyecto_techlab_springboot.salereturn.model.SaleReturn;
import com.proyecto.proyecto_techlab_springboot.salereturn.model.SaleReturnItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class SaleReturnMapper {

    public SaleReturnResponse toResponse(SaleReturn saleReturn) {
        List<SaleReturnItemResponse> itemResponses = saleReturn.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return SaleReturnResponse.builder()
                .returnId(saleReturn.getReturnId())
                .saleId(saleReturn.getSale().getSaleId())
                .trackingToken(saleReturn.getTrackingToken())
                .returnReason(saleReturn.getReturnReason())
                .returnResolution(saleReturn.getReturnResolution())
                .returnStatus(saleReturn.getReturnStatus())
                .shippingCoveredByStore(saleReturn.getShippingCoveredByStore())
                .returnShippingGuide(saleReturn.getReturnShippingGuide())
                .refundAmount(saleReturn.getRefundAmount())
                .customerNotes(saleReturn.getCustomerNotes())
                .inspectionNotes(saleReturn.getInspectionNotes())
                .inspectionEvidencePath(saleReturn.getInspectionEvidencePath())
                .inspectedBy(saleReturn.getInspectedBy() != null ? saleReturn.getInspectedBy().getUserId() : null)
                .inspectedAt(saleReturn.getInspectedAt())
                .createdAt(saleReturn.getCreatedAt())
                .updatedAt(saleReturn.getUpdatedAt())
                .closedAt(saleReturn.getClosedAt())
                .items(itemResponses)
                .build();
    }

    public SaleReturnSummaryResponse toSummaryResponse(SaleReturn saleReturn) {
        return SaleReturnSummaryResponse.builder()
                .returnId(saleReturn.getReturnId())
                .saleId(saleReturn.getSale().getSaleId())
                .trackingToken(saleReturn.getTrackingToken())
                .customerName(saleReturn.getSale().getCustomerName())
                .customerEmail(saleReturn.getSale().getCustomerEmail())
                .returnReason(saleReturn.getReturnReason())
                .returnResolution(saleReturn.getReturnResolution())
                .returnStatus(saleReturn.getReturnStatus())
                .refundAmount(saleReturn.getRefundAmount())
                .createdAt(saleReturn.getCreatedAt())
                .closedAt(saleReturn.getClosedAt())
                .build();
    }

    private SaleReturnItemResponse toItemResponse(SaleReturnItem item) {
        SaleItem saleItem = item.getSaleItem();
        return SaleReturnItemResponse.builder()
                .returnItemId(item.getReturnItemId())
                .saleItemId(saleItem.getSaleItemId())
                .variantId(saleItem.getVariant() != null ? saleItem.getVariant().getVariantId() : null)
                .productName(saleItem.getProductName())
                .variantDesc(saleItem.getVariantDesc())
                .variantSku(saleItem.getVariantSku())
                .variantSize(saleItem.getVariantSize())
                .colorName(saleItem.getColorName())
                .imageUrl(saleItem.getImageUrl())
                .unitPrice(saleItem.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(saleItem.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .restockApproved(item.getRestockApproved())
                .rejectionReason(item.getRejectionReason())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
