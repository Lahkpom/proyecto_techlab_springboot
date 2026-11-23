package com.proyecto.proyecto_techlab_springboot.sale.mapper;

import com.proyecto.proyecto_techlab_springboot.sale.dto.SaleItemResponse;
import com.proyecto.proyecto_techlab_springboot.sale.dto.SaleResponse;
import com.proyecto.proyecto_techlab_springboot.sale.dto.SaleSummaryResponse;
import com.proyecto.proyecto_techlab_springboot.sale.model.Sale;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SaleMapper {

    public SaleResponse toResponse(Sale sale, String mpInitPoint) {
        List<SaleItemResponse> itemResponses = sale.getItems().stream()
                .map(item -> SaleItemResponse.builder()
                        .saleItemId(item.getSaleItemId())
                        .variantId(item.getVariant() != null ? item.getVariant().getVariantId() : null)
                        .productName(item.getProductName())
                        .productDesc(item.getProductDesc())
                        .variantDesc(item.getVariantDesc())
                        .variantSku(item.getVariantSku())
                        .variantSize(item.getVariantSize())
                        .colorName(item.getColorName())
                        .imageUrl(item.getImageUrl())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .createdAt(item.getCreatedAt())
                        .build())
                .toList();

        return SaleResponse.builder()
                .saleId(sale.getSaleId())
                .userId(sale.getUser() != null ? sale.getUser().getUserId() : null)
                .trackingToken(sale.getTrackingToken())
                .customerName(sale.getCustomerName())
                .customerEmail(sale.getCustomerEmail())
                .customerTaxIdType(sale.getCustomerTaxIdType())
                .customerTaxIdNumber(sale.getCustomerTaxIdNumber())
                .customerPhone(sale.getCustomerPhone())
                .customerNotes(sale.getCustomerNotes())
                .shippingMethod(sale.getShippingMethod())
                .shippingProvince(sale.getShippingProvince())
                .shippingLocality(sale.getShippingLocality())
                .shippingPostalCode(sale.getShippingPostalCode())
                .shippingAddress(sale.getShippingAddress())
                .saleQuantity(sale.getSaleQuantity())
                .saleSubtotal(sale.getSaleSubtotal())
                .saleDiscountAmount(sale.getSaleDiscountAmount())
                .shippingCost(sale.getShippingCost())
                .saleTotal(sale.getSaleTotal())
                .saleShippedAt(sale.getSaleShippedAt())
                .saleDeliveredAt(sale.getSaleDeliveredAt())
                .saleStatus(sale.getSaleStatus())
                .paymentMethod(sale.getPaymentMethod())
                .paymentStatus(sale.getPaymentStatus())
                .fiscalRejection(sale.isFiscalRejection())
                .internalNotes(sale.getInternalNotes())
                .mpPreferenceId(sale.getMpPreferenceId())
                .mpInitPoint(mpInitPoint)
                .createdAt(sale.getCreatedAt())
                .updatedAt(sale.getUpdatedAt())
                .items(itemResponses)
                .build();
    }

    public SaleSummaryResponse toSummaryResponse(Sale sale) {
        return SaleSummaryResponse.builder()
                .saleId(sale.getSaleId())
                .userId(sale.getUser() != null ? sale.getUser().getUserId() : null)
                .trackingToken(sale.getTrackingToken())
                .customerName(sale.getCustomerName())
                .customerEmail(sale.getCustomerEmail())
                .customerTaxIdType(sale.getCustomerTaxIdType())
                .customerTaxIdNumber(sale.getCustomerTaxIdNumber())
                .shippingMethod(sale.getShippingMethod())
                .shippingProvince(sale.getShippingProvince())
                .shippingLocality(sale.getShippingLocality())
                .shippingPostalCode(sale.getShippingPostalCode())
                .saleQuantity(sale.getSaleQuantity())
                .saleTotal(sale.getSaleTotal())
                .saleStatus(sale.getSaleStatus())
                .paymentMethod(sale.getPaymentMethod())
                .paymentStatus(sale.getPaymentStatus())
                .fiscalRejection(sale.isFiscalRejection())
                .saleDeliveredAt(sale.getSaleDeliveredAt())
                .createdAt(sale.getCreatedAt())
                .build();
    }
}
