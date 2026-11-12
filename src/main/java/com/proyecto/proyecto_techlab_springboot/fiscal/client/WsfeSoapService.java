package com.proyecto.proyecto_techlab_springboot.fiscal.client;

import com.proyecto.proyecto_techlab_springboot.fiscal.config.ArcaProperties;
import com.proyecto.proyecto_techlab_springboot.fiscal.exception.ArcaBusinessRejectionException;
import com.proyecto.proyecto_techlab_springboot.fiscal.exception.ArcaUnavailableException;
import com.proyecto.proyecto_techlab_springboot.fiscal.model.FiscalReceiptType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class WsfeSoapService implements IWsfeSoapClient {

    private static final DateTimeFormatter AFIP_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final IWsaaTokenClient  wsaaTokenClient;
    private final ArcaProperties    arcaProperties;

    /**
     * Consulta a ARCA el último número de comprobante autorizado para el punto de venta y tipo.
     */
    @Override
    public long getLastAuthorizedNumber(int pointOfSale, FiscalReceiptType receiptType) {
        try {
            IWsaaTokenClient.AccessTicket ticket = this.wsaaTokenClient.getAccessTicket();
            String soapRequest  = buildLastNumberSoapEnvelope(ticket, pointOfSale, receiptType.getAfipCode());
            String soapResponse = callSoapHttp(soapRequest, "http://ar.gov.afip.dif.FEV1/FECompUltimoAutorizado");
            return parseLastNumberResponse(soapResponse);
        } catch (ArcaBusinessRejectionException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Fallo al consultar FECompUltimoAutorizado en ARCA: {}", ex.getMessage(), ex);
            throw new ArcaUnavailableException("Error de comunicación/infraestructura en ARCA: " + ex.getMessage(), ex);
        }
    }

    /**
     * Solicita a ARCA la autorización (CAE) de un comprobante tipo C (Factura C o Nota de Crédito C).
     */
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
        try {
            IWsaaTokenClient.AccessTicket ticket = this.wsaaTokenClient.getAccessTicket();
            String soapRequest = buildAuthorizeSoapEnvelope(
                    ticket, pointOfSale, receiptType.getAfipCode(), nextNumber, issueDate,
                    docType, docNumber, totalAmount, associatedReceipt
            );

            String soapResponse = callSoapHttp(soapRequest, "http://ar.gov.afip.dif.FEV1/FECAESolicitar");
            return parseAuthorizeResponse(soapResponse);
        } catch (ArcaBusinessRejectionException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Fallo al autorizar comprobante en ARCA WSFE: {}", ex.getMessage(), ex);
            throw new ArcaUnavailableException("Error en comunicación con ARCA WSFE: " + ex.getMessage(), ex);
        }
    }

    private String buildLastNumberSoapEnvelope(IWsaaTokenClient.AccessTicket ticket, int pos, int cbteTipo) {
        return """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ar="http://ar.gov.afip.dif.FEV1/">
           <soapenv:Header/>
           <soapenv:Body>
              <ar:FECompUltimoAutorizado>
                 <ar:Auth>
                    <ar:Token>%s</ar:Token>
                    <ar:Sign>%s</ar:Sign>
                    <ar:Cuit>%s</ar:Cuit>
                 </ar:Auth>
                 <ar:PtoVta>%d</ar:PtoVta>
                 <ar:CbteTipo>%d</ar:CbteTipo>
              </ar:FECompUltimoAutorizado>
           </soapenv:Body>
        </soapenv:Envelope>
        """.formatted(ticket.getToken(), ticket.getSign(), arcaProperties.cuit(), pos, cbteTipo);
    }

    private String buildAuthorizeSoapEnvelope(
            IWsaaTokenClient.AccessTicket ticket,
            int                           pos,
            int                           cbteTipo,
            long                          cbteNro,
            LocalDate                     issueDate,
            int                           docType,
            String                        docNumber,
            BigDecimal                    totalAmount,
            AssociatedReceiptInfo         associatedReceipt
    ) {
        String formattedDate   = issueDate.format(AFIP_DATE_FORMATTER);
        String formattedAmount = totalAmount.setScale(2, RoundingMode.HALF_UP).toPlainString();

        String cbtesAsocNode = "";
        if (associatedReceipt != null) {
            cbtesAsocNode = """
                <ar:CbtesAsoc>
                    <ar:CbteAsoc>
                        <ar:Tipo>%d</ar:Tipo>
                        <ar:PtoVta>%d</ar:PtoVta>
                        <ar:Nro>%d</ar:Nro>
                    </ar:CbteAsoc>
                </ar:CbtesAsoc>
            """.formatted(
                    associatedReceipt.getReceiptType(),
                    associatedReceipt.getPointOfSale(),
                    associatedReceipt.getReceiptNumber()
            );
        }

        return """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ar="http://ar.gov.afip.dif.FEV1/">
           <soapenv:Header/>
           <soapenv:Body>
              <ar:FECAESolicitar>
                 <ar:Auth>
                    <ar:Token>%s</ar:Token>
                    <ar:Sign>%s</ar:Sign>
                    <ar:Cuit>%s</ar:Cuit>
                 </ar:Auth>
                 <ar:FeCAEReq>
                    <ar:FeCabReq>
                       <ar:CantReg>1</ar:CantReg>
                       <ar:PtoVta>%d</ar:PtoVta>
                       <ar:CbteTipo>%d</ar:CbteTipo>
                    </ar:FeCabReq>
                    <ar:FeDetReq>
                       <ar:FECAEDetRequest>
                          <ar:Concepto>1</ar:Concepto>
                          <ar:DocTipo>%d</ar:DocTipo>
                          <ar:DocNro>%s</ar:DocNro>
                          <ar:CbteDesde>%d</ar:CbteDesde>
                          <ar:CbteHasta>%d</ar:CbteHasta>
                          <ar:CbteFch>%s</ar:CbteFch>
                          <ar:ImpTotal>%s</ar:ImpTotal>
                          <ar:ImpTotConc>0.00</ar:ImpTotConc>
                          <ar:ImpNeto>%s</ar:ImpNeto>
                          <ar:ImpOpEx>0.00</ar:ImpOpEx>
                          <ar:ImpTrib>0.00</ar:ImpTrib>
                          <ar:ImpIVA>0.00</ar:ImpIVA>
                          <ar:MonId>PES</ar:MonId>
                          <ar:MonCotiz>1.00</ar:MonCotiz>
                          %s
                       </ar:FECAEDetRequest>
                    </ar:FeDetReq>
                 </ar:FeCAEReq>
              </ar:FECAESolicitar>
           </soapenv:Body>
        </soapenv:Envelope>
        """.formatted(
                ticket.getToken(), ticket.getSign(), arcaProperties.cuit(),
                pos, cbteTipo,
                docType, docNumber, cbteNro, cbteNro, formattedDate,
                formattedAmount, formattedAmount,
                cbtesAsocNode
        );
    }

    private String callSoapHttp(String soapEnvelope, String soapAction) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(arcaProperties.wsfeUrl()))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "text/xml; charset=UTF-8")
                .header("SOAPAction", soapAction)
                .POST(HttpRequest.BodyPublishers.ofString(soapEnvelope))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200)
            throw new ArcaUnavailableException("Respuesta HTTP anómala de ARCA WSFE [Status %d]: %s.".formatted(response.statusCode(), response.body()), null);

        return response.body();
    }

    private long parseLastNumberResponse(String soapXml) throws Exception {
        DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();
        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(soapXml)));

        NodeList errors = doc.getElementsByTagName("Err");
        if (errors.getLength() > 0) {
            String errMsg = errors.item(0).getTextContent();
            throw new ArcaBusinessRejectionException("ARCA reportó error en FECompUltimoAutorizado: " + errMsg);
        }

        String cbteNro = doc.getElementsByTagName("CbteNro").item(0).getTextContent();
        return Long.parseLong(cbteNro.trim());
    }

    private CaeAuthorizationResult parseAuthorizeResponse(String soapXml) throws Exception {
        DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();
        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(soapXml)));

        // Validar errores generales en cabecera
        NodeList errors = doc.getElementsByTagName("Err");
        if (errors.getLength() > 0) {
            String msg = errors.item(0).getTextContent();
            throw new ArcaBusinessRejectionException("ARCA rechazó el comprobante por error de validación: " + msg);
        }

        String resultado = doc.getElementsByTagName("Resultado").item(0).getTextContent();
        if (resultado.equalsIgnoreCase("R")) {
            NodeList obs = doc.getElementsByTagName("Obs");
            String obsMsg = (obs.getLength() > 0) ? obs.item(0).getTextContent() : "Sin observaciones devueltas";
            throw new ArcaBusinessRejectionException("Comprobante RECHAZADO por ARCA. Motivo: " + obsMsg);
        }

        String cae       = doc.getElementsByTagName("CAE").item(0).getTextContent();
        String caeFchVto = doc.getElementsByTagName("CAEFchVto").item(0).getTextContent();
        String cbteDesde = doc.getElementsByTagName("CbteDesde").item(0).getTextContent();

        return CaeAuthorizationResult.builder()
                .cae(cae.trim())
                .caeDueDate(LocalDate.parse(caeFchVto.trim(), AFIP_DATE_FORMATTER))
                .receiptNumber(Long.parseLong(cbteDesde.trim()))
                .result(resultado.trim())
                .build();
    }

    private DocumentBuilderFactory createSecureDocumentBuilderFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }
}