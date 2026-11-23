package com.proyecto.proyecto_techlab_springboot.fiscal.service;

import com.proyecto.proyecto_techlab_springboot.fiscal.exception.FiscalReceiptNotFoundException;
import com.proyecto.proyecto_techlab_springboot.fiscal.dto.FiscalReceiptResponse;
import com.proyecto.proyecto_techlab_springboot.fiscal.mapper.FiscalReceiptMapper;
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
    private final FiscalReceiptService      fiscalReceiptService;
    private final FiscalReceiptMapper       fiscalReceiptMapper;

    @Transactional(readOnly = true)
    public List<FiscalReceiptResponse> getReceiptsBySale(UUID saleId) {
        this.saleService.getSaleByIdOrElseThrow(saleId);

        return this.fiscalReceiptRepository.findAllBySale_SaleId(saleId).stream()
                .map(this.fiscalReceiptMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FiscalReceiptResponse getReceiptDetail(UUID fiscalReceiptId) {
        FiscalReceipt receipt = this.fiscalReceiptRepository.findById(fiscalReceiptId)
                .orElseThrow(() -> new FiscalReceiptNotFoundException("{ ID: %s }".formatted(fiscalReceiptId)));

        return this.fiscalReceiptMapper.toResponse(receipt);
    }

    @Transactional
    public FiscalReceiptResponse retryRejectedInvoiceForSale(UUID saleId, UUID operatorId) {
        return this.fiscalReceiptMapper.toResponse(
                this.fiscalReceiptService.retryRejectedInvoiceForSale(saleId, operatorId)
        );
    }
}