package com.proyecto.proyecto_techlab_springboot.fiscal.mapper;

import com.proyecto.proyecto_techlab_springboot.fiscal.dto.FiscalReceiptResponse;
import com.proyecto.proyecto_techlab_springboot.fiscal.model.FiscalReceipt;
import org.springframework.stereotype.Component;

@Component
public class FiscalReceiptMapper {

    public FiscalReceiptResponse toResponse(FiscalReceipt receipt) {
        String formattedNumber = "%05d-%08d".formatted(receipt.getPointOfSale(), receipt.getReceiptNumber());

        return FiscalReceiptResponse.builder()
                .fiscalReceiptId(receipt.getFiscalReceiptId())
                .saleId(receipt.getSale().getSaleId())
                .returnId(receipt.getSaleReturn() != null ? receipt.getSaleReturn().getReturnId() : null)
                .receiptType(receipt.getReceiptType())
                .pointOfSale(receipt.getPointOfSale())
                .receiptNumber(receipt.getReceiptNumber())
                .formattedReceiptNumber(formattedNumber)
                .cae(receipt.getCae())
                .caeDueDate(receipt.getCaeDueDate())
                .issueDate(receipt.getIssueDate())
                .docType(receipt.getDocType())
                .docNumber(receipt.getDocNumber())
                .customerName(receipt.getCustomerName())
                .totalAmount(receipt.getTotalAmount())
                .qrPayload(receipt.getQrPayload())
                .createdAt(receipt.getCreatedAt())
                .build();
    }
}
