package com.proyecto.proyecto_techlab_springboot.service.fiscal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.proyecto_techlab_springboot.model.*;
import com.proyecto.proyecto_techlab_springboot.model.enums.FiscalReceiptType;
import com.proyecto.proyecto_techlab_springboot.model.enums.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnResolution;
import com.proyecto.proyecto_techlab_springboot.model.enums.ReturnStatus;
import com.proyecto.proyecto_techlab_springboot.repository.IFiscalReceiptRepository;
import com.proyecto.proyecto_techlab_springboot.repository.ISaleReturnRepository;
import com.proyecto.proyecto_techlab_springboot.repository.IUserRepository;
import com.proyecto.proyecto_techlab_springboot.service.SaleReturnService;
import com.proyecto.proyecto_techlab_springboot.service.SaleService;
import com.proyecto.proyecto_techlab_springboot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Este servicio gestiona las transacciones, la lógica de negocio y la persistencia inmutable:
 *     - emitInvoiceForSale(UUID saleId, UUID operatorId): Emite la Factura C para una orden abonada.
 *     - emitCreditNoteForReturn(UUID returnId, UUID operatorId): Emite la Nota de Crédito C para una devolución aprobada, buscando y asociando la Factura C original.
 *     - Generador de QR oficial (RG 4291/2018): Arma el JSON base64 normado por ARCA para generar la URL oficial del código QR fiscal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FiscalReceiptService {

    private final IFiscalReceiptRepository  fiscalReceiptRepository;
    private final SaleService saleService;
    private final ISaleReturnRepository     saleReturnRepository;
    private final SaleReturnService saleReturnService;
    private final IUserRepository           userRepository;
    private final UserService userService;
    private final WsfeSoapService           wsfeSoapService;
    private final ObjectMapper              objectMapper;

    @Value("${arca.punto-venta}")
    private int puntoVenta;

    @Value("${arca.cuit}")
    private String cuitEmisor;

    @Value("${arca.mock-mode}")
    private boolean mockMode;

    /**
     * Emite la Factura C correspondiente a una venta cobrada.
     */
    @Transactional
    public FiscalReceipt emitInvoiceForSale(UUID saleId, UUID operatorId) {
        Sale sale = this.saleService.getSaleByIdOrElseThrow(saleId);

        if (sale.getPaymentStatus() != PaymentStatus.APROBADO)
            throw new IllegalStateException("Solo se pueden facturar ventas con pago APROBADO.");

        if (this.existsBySaleIdAndReceiptType(saleId, FiscalReceiptType.FACTURA_C))
            throw new IllegalStateException("La venta " + saleId + " ya cuenta con una Factura C emitida.");

        Users       operator    = getOperator(operatorId);
        LocalDate   today       = LocalDate.now();

        // Resolver receptor fiscal
        int     docType     = resolveDocType(sale);
        String  docNumber   = resolveDocNumber(sale, docType);

        // Determinar número correlativo
        long nextNumber = resolveNextNumber(puntoVenta, FiscalReceiptType.FACTURA_C);

        // Solicitar CAE a ARCA
        WsfeSoapService.CaeAuthorizationResult arcaResult = this.wsfeSoapService.authorizeReceipt(
                puntoVenta,
                FiscalReceiptType.FACTURA_C,
                nextNumber,
                today,
                docType,
                docNumber,
                sale.getSaleTotal(),
                null // Sin comprobantes asociados
        );

        // Armar QR oficial de ARCA
        String qrPayload = generateQrPayload(
                today,
                puntoVenta,
                FiscalReceiptType.FACTURA_C.getAfipCode(),
                arcaResult.getReceiptNumber(),
                sale.getSaleTotal(),
                docType,
                docNumber,
                arcaResult.getCae()
        );

        // Persistir comprobante inmutable
        FiscalReceipt receipt = FiscalReceipt.builder()
                .sale(sale)
                .receiptType(FiscalReceiptType.FACTURA_C)
                .afipCbteTipo(FiscalReceiptType.FACTURA_C.getAfipCode())
                .pointOfSale(puntoVenta)
                .receiptNumber(arcaResult.getReceiptNumber())
                .cae(arcaResult.getCae())
                .caeDueDate(arcaResult.getCaeDueDate())
                .issueDate(today)
                .docType(docType)
                .docNumber(docNumber)
                .customerName(sale.getCustomerName())
                .totalAmount(sale.getSaleTotal())
                .netAmount(sale.getSaleTotal())
                .qrPayload(qrPayload)
                .createdBy(operator)
                .build();

        FiscalReceipt saved = this.fiscalReceiptRepository.save(receipt);
        log.info("Factura C emitida exitosamente: PtoVta {}, Nro {}, CAE {}", puntoVenta, saved.getReceiptNumber(), saved.getCae());
        return saved;
    }

    private Users getOperator(UUID operatorId) {
        return (operatorId != null)
                ? this.userService.findUserById(operatorId).orElse(null)
                : null;
    }

    /**
     * Emite la Nota de Crédito C para una devolución con reintegro aprobada.
     */
    @Transactional
    public FiscalReceipt emitCreditNoteForReturn(UUID returnId, UUID operatorId) {
        SaleReturn saleReturn = this.saleReturnService.getSaleReturnByIdOrThrow(returnId);

        if (saleReturn.getReturnStatus() != ReturnStatus.APROBADA)
            throw new IllegalStateException("Solo se pueden emitir notas de crédito para devoluciones APROBADAS.");

        if (saleReturn.getReturnResolution() != ReturnResolution.REEMBOLSO_DINERO)
            throw new IllegalStateException("La devolución no tiene resolución monetaria (REEMBOLSO_DINERO).");

        if (this.fiscalReceiptRepository.findBySaleReturn_ReturnId(returnId).isPresent())
            throw new IllegalStateException("Ya existe una Nota de Crédito emitida para la devolución " + returnId);

        Sale sale = saleReturn.getSale();

        // Recuperar la Factura C original para asociarla ante ARCA
        FiscalReceipt originalInvoice = this.fiscalReceiptRepository
                .findBySale_SaleIdAndReceiptType(sale.getSaleId(), FiscalReceiptType.FACTURA_C)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la Factura C original para la venta " + sale.getSaleId() + ". No se puede asociar la Nota de Crédito en ARCA."
                ));

        Users       operator    = getOperator(operatorId);
        LocalDate   today       = LocalDate.now();

        // Resolver receptor y número
        int     docType     = originalInvoice.getDocType();
        String  docNumber   = originalInvoice.getDocNumber();
        long    nextNumber  = resolveNextNumber(puntoVenta, FiscalReceiptType.NOTA_CREDITO_C);

        WsfeSoapService.AssociatedReceiptInfo associatedReceipt = WsfeSoapService.AssociatedReceiptInfo.builder()
                .receiptType(originalInvoice.getAfipCbteTipo())
                .pointOfSale(originalInvoice.getPointOfSale())
                .receiptNumber(originalInvoice.getReceiptNumber())
                .build();

        // Solicitar CAE a ARCA
        WsfeSoapService.CaeAuthorizationResult arcaResult = this.wsfeSoapService.authorizeReceipt(
                puntoVenta,
                FiscalReceiptType.NOTA_CREDITO_C,
                nextNumber,
                today,
                docType,
                docNumber,
                saleReturn.getRefundAmount(),
                associatedReceipt
        );

        // Armar QR oficial
        String qrPayload = generateQrPayload(
                today,
                puntoVenta,
                FiscalReceiptType.NOTA_CREDITO_C.getAfipCode(),
                arcaResult.getReceiptNumber(),
                saleReturn.getRefundAmount(),
                docType,
                docNumber,
                arcaResult.getCae()
        );

        // Persistir Nota de Crédito inmutable
        FiscalReceipt creditNote = FiscalReceipt.builder()
                .sale(sale)
                .saleReturn(saleReturn)
                .receiptType(FiscalReceiptType.NOTA_CREDITO_C)
                .afipCbteTipo(FiscalReceiptType.NOTA_CREDITO_C.getAfipCode())
                .pointOfSale(puntoVenta)
                .receiptNumber(arcaResult.getReceiptNumber())
                .cae(arcaResult.getCae())
                .caeDueDate(arcaResult.getCaeDueDate())
                .issueDate(today)
                .docType(docType)
                .docNumber(docNumber)
                .customerName(sale.getCustomerName())
                .totalAmount(saleReturn.getRefundAmount())
                .netAmount(saleReturn.getRefundAmount())
                .qrPayload(qrPayload)
                .createdBy(operator)
                .build();

        FiscalReceipt saved = this.fiscalReceiptRepository.save(creditNote);
        log.info("Nota de Crédito C emitida exitosamente: PtoVta {}, Nro {}, CAE {}", puntoVenta, saved.getReceiptNumber(), saved.getCae());
        return saved;
    }

    private int resolveDocType(Sale sale) {
        if (sale.getCustomerTaxIdType() != null)
            return sale.getCustomerTaxIdType().getAfipCode();

        return 99; // Sin identificar / Consumidor Final
    }

    private String resolveDocNumber(Sale sale, int docType) {
        if (docType == 99 || sale.getCustomerTaxIdNumber() == null || sale.getCustomerTaxIdNumber().isBlank())
            return "0";

        return sale.getCustomerTaxIdNumber().trim().replaceAll("[^0-9]", "");
    }

    private long resolveNextNumber(int pos, FiscalReceiptType type) {
        long lastNumber = (mockMode)
                ? this.fiscalReceiptRepository.findMaxReceiptNumber(pos, type)
                : this.wsfeSoapService.getLastAuthorizedNumber(pos, type);

        return lastNumber + 1;
    }

    private String generateQrPayload(
            LocalDate   date,
            int         pos,
            int         cbteTipo,
            long        nro,
            BigDecimal  importe,
            int         docTipo,
            String      docNro,
            String      cae
    ) {
        // TODO - Ver este nested try-catch y mejorar el segundo
        try {
            long numericDocNro = 0L;
            try {
                numericDocNro = Long.parseLong(docNro);
            } catch (NumberFormatException ignored) {}

            Map<String, Object> qrData = Map.ofEntries(
                    Map.entry("ver"         , 1),
                    Map.entry("fecha"       , date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))),
                    Map.entry("cuit"        , Long.parseLong(cuitEmisor.replaceAll("[^0-9]", ""))),
                    Map.entry("ptoVta"      , pos),
                    Map.entry("tipoCmp"     , cbteTipo),
                    Map.entry("nroCmp"      , nro),
                    Map.entry("importe"     , importe.doubleValue()),
                    Map.entry("moneda"      , "PES"),
                    Map.entry("ctz"         , 1),
                    Map.entry("tipoDocRec"  , docTipo),
                    Map.entry("nroDocRec"   , numericDocNro),
                    Map.entry("tipoCodAut"  , "E"),
                    Map.entry("codAut"      , Long.parseLong(cae.trim()))
            );

            String json = objectMapper.writeValueAsString(qrData);
            String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            return "https://www.afip.gob.ar/fe/qr/?p=" + encoded;
        } catch (Exception ex) {
            log.warn("No se pudo armar el payload QR para comprobante #{}: {}", nro, ex.getMessage());
            return "";
        }
    }

    public boolean existsBySaleIdAndReceiptType(UUID saleId, FiscalReceiptType receiptType) {
        return this.fiscalReceiptRepository.existsBySale_SaleIdAndReceiptType(saleId, receiptType);
    }
}