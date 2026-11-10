package com.proyecto.proyecto_techlab_springboot.fiscal.service;

import com.proyecto.proyecto_techlab_springboot.fiscal.dto.FiscalReceiptResponse;
import com.proyecto.proyecto_techlab_springboot.fiscal.exception.FiscalReceiptNotFoundException;
import com.proyecto.proyecto_techlab_springboot.fiscal.model.FiscalReceipt;
import com.proyecto.proyecto_techlab_springboot.fiscal.repository.IFiscalReceiptRepository;
import com.proyecto.proyecto_techlab_springboot.sale.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FiscalReceiptAdminService {

    private final IFiscalReceiptRepository fiscalReceiptRepository;
    private final SaleService              saleService;

    @Transactional(readOnly = true)
    public List<FiscalReceiptResponse> getReceiptsBySale(UUID saleId) {
        this.saleService.getSaleByIdOrElseThrow(saleId);

        return this.fiscalReceiptRepository.findAllBySale_SaleId(saleId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FiscalReceiptResponse getReceiptDetail(UUID fiscalReceiptId) {
        FiscalReceipt receipt = this.fiscalReceiptRepository.findById(fiscalReceiptId)
                .orElseThrow(() -> new FiscalReceiptNotFoundException("{ ID: %s }".formatted(fiscalReceiptId)));

        return mapToResponse(receipt);
    }

    private FiscalReceiptResponse mapToResponse(FiscalReceipt r) {
        String formattedNumber = "%05d-%08d".formatted(r.getPointOfSale(), r.getReceiptNumber());

        return FiscalReceiptResponse.builder()
                .fiscalReceiptId(r.getFiscalReceiptId())
                .saleId(r.getSale().getSaleId())
                .returnId(r.getSaleReturn() != null ? r.getSaleReturn().getReturnId() : null)
                .receiptType(r.getReceiptType())
                .pointOfSale(r.getPointOfSale())
                .receiptNumber(r.getReceiptNumber())
                .formattedReceiptNumber(formattedNumber)
                .cae(r.getCae())
                .caeDueDate(r.getCaeDueDate())
                .issueDate(r.getIssueDate())
                .docType(r.getDocType())
                .docNumber(r.getDocNumber())
                .customerName(r.getCustomerName())
                .totalAmount(r.getTotalAmount())
                .qrPayload(r.getQrPayload())
                .createdAt(r.getCreatedAt())
                .build();
    }
}