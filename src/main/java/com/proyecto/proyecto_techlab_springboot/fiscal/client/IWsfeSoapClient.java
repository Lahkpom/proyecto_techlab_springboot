package com.proyecto.proyecto_techlab_springboot.fiscal.client;

import com.proyecto.proyecto_techlab_springboot.fiscal.model.FiscalReceiptType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface IWsfeSoapClient {

    long getLastAuthorizedNumber(int pointOfSale, FiscalReceiptType receiptType);

    CaeAuthorizationResult authorizeReceipt(
            int                     pointOfSale,
            FiscalReceiptType       receiptType,
            long                    nextNumber,
            LocalDate               issueDate,
            int                     docType,
            String                  docNumber,
            BigDecimal              totalAmount,
            AssociatedReceiptInfo   associatedReceipt
    );

    @Getter
    @Builder
    class CaeAuthorizationResult {
        private String      cae;
        private LocalDate   caeDueDate;
        private Long        receiptNumber;
        private String      result;
    }

    @Getter
    @Builder
    class AssociatedReceiptInfo {
        private Integer receiptType;
        private Integer pointOfSale;
        private Long    receiptNumber;
    }
}