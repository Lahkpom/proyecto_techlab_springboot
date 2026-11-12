package com.proyecto.proyecto_techlab_springboot.fiscal.client;

import com.proyecto.proyecto_techlab_springboot.fiscal.model.FiscalReceiptType;
import com.proyecto.proyecto_techlab_springboot.fiscal.repository.IFiscalReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class MockWsfeSoapService implements IWsfeSoapClient {

    private final IFiscalReceiptRepository fiscalReceiptRepository;

    @Override
    public long getLastAuthorizedNumber(int pointOfSale, FiscalReceiptType receiptType) {
        log.info("[ARCA MOCK - DEV] Consultando último comprobante local para POS: {}, Tipo: {}", pointOfSale, receiptType);
        return this.fiscalReceiptRepository.findMaxReceiptNumber(pointOfSale, receiptType);
    }

    @Override
    public CaeAuthorizationResult authorizeReceipt(
            int                   pointOfSale,
            FiscalReceiptType     receiptType,
            long                  nextNumber,
            LocalDate             issueDate,
            int                   docType,
            String                docNumber,
            BigDecimal            totalAmount,
            AssociatedReceiptInfo associatedReceipt
    ) {
        log.info("[ARCA MOCK - DEV] Autorizando comprobante simulado #{} (Tipo: {}, Monto: ${})", nextNumber, receiptType, totalAmount);
        String mockCae = String.valueOf(ThreadLocalRandom.current().nextLong(10000000000000L, 99999999999999L));

        return CaeAuthorizationResult.builder()
                .cae(mockCae)
                .caeDueDate(issueDate.plusDays(10))
                .receiptNumber(nextNumber)
                .result("A")
                .build();
    }
}