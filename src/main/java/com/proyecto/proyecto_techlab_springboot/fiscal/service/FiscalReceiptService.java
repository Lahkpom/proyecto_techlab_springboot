package com.proyecto.proyecto_techlab_springboot.fiscal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.proyecto_techlab_springboot.fiscal.client.IWsfeSoapClient;
import com.proyecto.proyecto_techlab_springboot.fiscal.config.ArcaProperties;
import com.proyecto.proyecto_techlab_springboot.fiscal.exception.ArcaBusinessRejectionException;
import com.proyecto.proyecto_techlab_springboot.fiscal.exception.ArcaUnavailableException;
import com.proyecto.proyecto_techlab_springboot.fiscal.exception.FiscalReceiptAlreadyExistsException;
import com.proyecto.proyecto_techlab_springboot.fiscal.exception.InvalidFiscalOperationException;
import com.proyecto.proyecto_techlab_springboot.fiscal.model.FiscalReceipt;
import com.proyecto.proyecto_techlab_springboot.fiscal.model.FiscalReceiptType;
import com.proyecto.proyecto_techlab_springboot.fiscal.repository.IFiscalReceiptRepository;
import com.proyecto.proyecto_techlab_springboot.sale.model.PaymentStatus;
import com.proyecto.proyecto_techlab_springboot.sale.model.Sale;
import com.proyecto.proyecto_techlab_springboot.sale.service.SaleService;
import com.proyecto.proyecto_techlab_springboot.salereturn.model.ReturnResolution;
import com.proyecto.proyecto_techlab_springboot.salereturn.model.ReturnStatus;
import com.proyecto.proyecto_techlab_springboot.salereturn.model.SaleReturn;
import com.proyecto.proyecto_techlab_springboot.salereturn.service.SaleReturnService;
import com.proyecto.proyecto_techlab_springboot.user.model.Users;
import com.proyecto.proyecto_techlab_springboot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final IFiscalReceiptRepository iFiscalReceiptRepository;

    private final SaleService       saleService;
    private final SaleReturnService saleReturnService;
    private final UserService       userService;
    private final IWsfeSoapClient   wsfeSoapClient;
    private final ObjectMapper      objectMapper;
    private final ArcaProperties    arcaProperties;

    /**
     * Disparo seguro para flujos transaccionales de cobro (Mercado Pago / Admin).
     * Garantiza que NINGUNA caída o rechazo de ARCA aborte el cobro de la orden.
     */
    @Transactional
    public void tryEmitInvoiceSafely(UUID saleId, UUID operatorId) {
        try {
            this.emitInvoiceForSale(saleId, operatorId);
        } catch (ArcaBusinessRejectionException ex) {
            log.error("[RECHAZO FISCAL]: Venta ID {} rechazada por ARCA. Detalle: {}", saleId, ex.getMessage());
        } catch (ArcaUnavailableException ex) {
            log.warn("[ARCA NO DISPONIBLE]: Servidor de ARCA inaccesible al cobrar Venta ID {}. La orden queda para reintento por Scheduler.", saleId);
        } catch (Exception ex) {
            log.warn("[ALERTA FISCAL]: Error inesperado al emitir Factura C para Venta ID {}: {}", saleId, ex.getMessage());
        }
    }

    /**
     * Disparo específico para el Scheduler de reintentos.
     * Relanza ArcaUnavailableException para que el bucle del scheduler se detenga de inmediato.
     */
    @Transactional
    public void retryInvoiceFromScheduler(UUID saleId) {
        try {
            this.emitInvoiceForSale(saleId, null);
        } catch (ArcaBusinessRejectionException ex) {
            log.error("[SCHEDULER - RECHAZO FISCAL]: Venta ID {} rechazada por ARCA. Se excluye de futuros reintentos. Detalle: {}", saleId, ex.getMessage());
        }
        // Si lanza ArcaUnavailableException, NO se atrapa acá:
        // sube directamente al catch(ArcaUnavailableException) del Scheduler para ejecutar el break.
    }

    /**
     * Emite la Factura C correspondiente a una venta cobrada.
     */
    private FiscalReceipt emitInvoiceForSale(UUID saleId, UUID operatorId) {
        Sale sale = this.saleService.getSaleByIdOrElseThrow(saleId);

        if (sale.getPaymentStatus() != PaymentStatus.APROBADO)
            throw new InvalidFiscalOperationException("Solo se pueden facturar ventas con pago APROBADO. Estado actual: " + sale.getPaymentStatus());

        if (this.existsBySaleIdAndReceiptType(saleId, FiscalReceiptType.FACTURA_C))
            throw new FiscalReceiptAlreadyExistsException("{ Venta ID: %s, Tipo: %s }".formatted(saleId, FiscalReceiptType.FACTURA_C));

        Users     operator = getOperator(operatorId);
        LocalDate today    = LocalDate.now();

        // Resolver receptor fiscal
        int    docType    = resolveDocType(sale);
        String docNumber  = resolveDocNumber(sale, docType);

        // Determinar número correlativo
        long   nextNumber = resolveNextNumber(arcaProperties.puntoVenta(), FiscalReceiptType.FACTURA_C);

        IWsfeSoapClient.CaeAuthorizationResult arcaResult;
        try {
            // Solicitar CAE a ARCA
            arcaResult = this.wsfeSoapClient.authorizeReceipt(
                    arcaProperties.puntoVenta(),
                    FiscalReceiptType.FACTURA_C,
                    nextNumber,
                    today,
                    docType,
                    docNumber,
                    sale.getSaleTotal(),
                    null
            );
        } catch (ArcaBusinessRejectionException ex) {
            this.saleService.markSaleAsFiscalRejected(saleId, ex.getMessage());
            throw ex;
        }

        // Armar QR oficial de ARCA
        String qrPayload = generateQrPayload(
                today,
                arcaProperties.puntoVenta(),
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
                .pointOfSale(arcaProperties.puntoVenta())
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

        FiscalReceipt saved = this.iFiscalReceiptRepository.save(receipt);
        log.info("Factura C emitida exitosamente: PtoVta {}, Nro {}, CAE {}", arcaProperties.puntoVenta(), saved.getReceiptNumber(), saved.getCae());
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
            throw new InvalidFiscalOperationException("Solo se pueden emitir notas de crédito para devoluciones APROBADAS. Estado actual: " + saleReturn.getReturnStatus());

        if (saleReturn.getReturnResolution() != ReturnResolution.REEMBOLSO_DINERO)
            throw new InvalidFiscalOperationException("La devolución no tiene resolución monetaria (REEMBOLSO_DINERO). Resolución actual: " + saleReturn.getReturnResolution());

        if (this.iFiscalReceiptRepository.findBySaleReturn_ReturnId(returnId).isPresent())
            throw new FiscalReceiptAlreadyExistsException("{ Devolución ID: %s, Tipo: %s }".formatted(returnId, FiscalReceiptType.NOTA_CREDITO_C));

        Sale sale = saleReturn.getSale();

        // Recuperar la Factura C original para asociarla ante ARCA
        FiscalReceipt originalInvoice = this.iFiscalReceiptRepository
                .findBySale_SaleIdAndReceiptType(sale.getSaleId(), FiscalReceiptType.FACTURA_C)
                .orElseThrow(() -> new InvalidFiscalOperationException(
                        "No se encontró la Factura C original para la venta %s. No se puede asociar la Nota de Crédito en ARCA.".formatted(sale.getSaleId())
                ));

        Users     operator   = getOperator(operatorId);
        LocalDate today      = LocalDate.now();

        // Resolver receptor y número
        int       docType    = originalInvoice.getDocType();
        String    docNumber  = originalInvoice.getDocNumber();
        long      nextNumber = resolveNextNumber(arcaProperties.puntoVenta(), FiscalReceiptType.NOTA_CREDITO_C);

        IWsfeSoapClient.AssociatedReceiptInfo associatedReceipt = IWsfeSoapClient.AssociatedReceiptInfo.builder()
                .receiptType(originalInvoice.getAfipCbteTipo())
                .pointOfSale(originalInvoice.getPointOfSale())
                .receiptNumber(originalInvoice.getReceiptNumber())
                .build();

        // Solicitar CAE a ARCA
        IWsfeSoapClient.CaeAuthorizationResult arcaResult = this.wsfeSoapClient.authorizeReceipt(
                arcaProperties.puntoVenta(),
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
                arcaProperties.puntoVenta(),
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
                .pointOfSale(arcaProperties.puntoVenta())
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

        FiscalReceipt saved = this.iFiscalReceiptRepository.save(creditNote);
        log.info("Nota de Crédito C emitida exitosamente: PtoVta {}, Nro {}, CAE {}", arcaProperties.puntoVenta(), saved.getReceiptNumber(), saved.getCae());
        return saved;
    }

    private int resolveDocType(Sale sale) {
        return (sale.getCustomerTaxIdType() != null)
                ? sale.getCustomerTaxIdType().getAfipCode()
                : 99; // Sin identificar / Consumidor Final
    }

    private String resolveDocNumber(Sale sale, int docType) {
        if (docType == 99 || sale.getCustomerTaxIdNumber() == null || sale.getCustomerTaxIdNumber().isBlank())
            return "0";

        return sale.getCustomerTaxIdNumber().trim().replaceAll("[^0-9]", "");
    }

    private long resolveNextNumber(int pos, FiscalReceiptType type) {
        return this.wsfeSoapClient.getLastAuthorizedNumber(pos, type) + 1;
    }

    private String generateQrPayload(
            LocalDate  date,
            int        pos,
            int        cbteTipo,
            long       nro,
            BigDecimal importe,
            int        docTipo,
            String     docNro,
            String     cae
    ) {
        try {
            long numericDocNro = parseLongOrDefault(docNro, 0L);

            Map<String, Object> qrData = Map.ofEntries(
                    Map.entry("ver"       , 1),
                    Map.entry("fecha"     , date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))),
                    Map.entry("cuit"      , Long.parseLong(arcaProperties.cuit().replaceAll("[^0-9]", ""))),
                    Map.entry("ptoVta"    , pos),
                    Map.entry("tipoCmp"   , cbteTipo),
                    Map.entry("nroCmp"    , nro),
                    Map.entry("importe"   , importe.doubleValue()),
                    Map.entry("moneda"    , "PES"),
                    Map.entry("ctz"       , 1),
                    Map.entry("tipoDocRec", docTipo),
                    Map.entry("nroDocRec" , numericDocNro),
                    Map.entry("tipoCodAut", "E"),
                    Map.entry("codAut"    , Long.parseLong(cae.trim()))
            );

            String json    = objectMapper.writeValueAsString(qrData);
            String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            return "https://www.afip.gob.ar/fe/qr/?p=" + encoded;
        } catch (Exception ex) {
            log.warn("No se pudo armar el payload QR para comprobante #{}: {}", nro, ex.getMessage());
            return "";
        }
    }

    private long parseLongOrDefault(String value, long defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;

        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public boolean existsBySaleIdAndReceiptType(UUID saleId, FiscalReceiptType receiptType) {
        return this.iFiscalReceiptRepository.existsBySale_SaleIdAndReceiptType(saleId, receiptType);
    }
}