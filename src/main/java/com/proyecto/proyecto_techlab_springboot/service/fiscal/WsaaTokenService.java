package com.proyecto.proyecto_techlab_springboot.service.fiscal;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Component 
public class WsaaTokenService {

    @Value("${arca.mock-mode}")
    private boolean mockMode;

    @Value("${arca.wsaa-url}")
    private String wsaaUrl;

    @Value("${arca.cert-path}")
    private String certPath;

    @Value("${arca.key-path}")
    private String keyPath;

    private AccessTicket cachedTicket;

    @Getter
    @Builder
    public static class AccessTicket {
        private String  token;
        private String  sign;
        private Instant expiration;

        // Se considera válido si le quedan más de 10 minutos de vigencia
        public boolean isValid() { return expiration != null && Instant.now().plus(10, ChronoUnit.MINUTES).isBefore(expiration); }
    }

    /**
     * Obtiene un Ticket de Acceso vigente para el servicio WSFE.
     */
    public synchronized AccessTicket getAccessTicket() {
        if (cachedTicket != null && cachedTicket.isValid()) return cachedTicket;

        if (mockMode) {
            log.info("[ARCA MOCK] Generando Ticket de Acceso simulado (Token y Sign ficticios).");
            this.cachedTicket = AccessTicket.builder()
                    .token("MOCK_TOKEN_ARCA_" + UUID.randomUUID())
                    .sign("MOCK_SIGN_ARCA_" + UUID.randomUUID())
                    .expiration(Instant.now().plus(12, ChronoUnit.HOURS))
                    .build();
            return this.cachedTicket;
        }

        log.info("Solicitando nuevo Ticket de Acceso a WSAA ({})", wsaaUrl);
        try {
            String loginTicketRequest   = buildLoginTicketRequestXml();
            byte[] signedCms            = signCms(loginTicketRequest.getBytes(StandardCharsets.UTF_8));
            String cmsBase64            = Base64.getEncoder().encodeToString(signedCms);

            String soapRequest  = buildSoapEnvelope(cmsBase64);
            String soapResponse = callWsaaHttp(soapRequest);

            this.cachedTicket = parseWsaaResponse(soapResponse);
            log.info("Ticket de Acceso obtenido exitosamente. Vence: {}", this.cachedTicket.getExpiration());
            return this.cachedTicket;

        } catch (Exception ex) {
            log.error("Fallo crítico al autenticar con ARCA WSAA: {}", ex.getMessage(), ex);
            throw new RuntimeException("Error de autenticación con el servicio fiscal ARCA WSAA: " + ex.getMessage(), ex);
        }
    }

    private String buildLoginTicketRequestXml() {
        Instant now     = Instant.now();
        Instant genTime = now.minus(10, ChronoUnit.MINUTES); // Mitigación de desincronización horaria (skew)
        Instant expTime = now.plus(12, ChronoUnit.HOURS);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<loginTicketRequest version=\"1.0\">"
                    + "<header>"
                        + "<uniqueId>" + now.getEpochSecond() + "</uniqueId>"
                        + "<generationTime>" + genTime + "</generationTime>"
                        + "<expirationTime>" + expTime + "</expirationTime>"
                    + "</header>"
                    + "<service>wsfe</service>"
                + "</loginTicketRequest>";
    }

    private byte[] signCms(byte[] contentToSign) throws Exception {
        X509Certificate certificate = loadCertificate(certPath);
        PrivateKey      privateKey  = loadPrivateKey(keyPath);

        CMSSignedDataGenerator  generator   = new CMSSignedDataGenerator();
        ContentSigner           signer      = new JcaContentSignerBuilder("SHA256withRSA").build(privateKey);

        generator.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder().build()
                ).build(signer, certificate)
        );

        generator.addCertificates(new JcaCertStore(Collections.singletonList(certificate)));

        CMSSignedData signedData = generator.generate(
                new CMSProcessableByteArray(PKCSObjectIdentifiers.data, contentToSign),
                true
        );

        return signedData.getEncoded();
    }

    private X509Certificate loadCertificate(String path) throws Exception {
        try (InputStream is = Files.newInputStream(Path.of(path))) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(is);
        }
    }

    private PrivateKey loadPrivateKey(String path) throws Exception {
        String keyContent = Files.readString(Path.of(path), StandardCharsets.UTF_8)
                .replaceAll("-----(BEGIN|END)( RSA)? PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[]              keyBytes    = Base64.getDecoder().decode(keyContent);
        PKCS8EncodedKeySpec spec        = new PKCS8EncodedKeySpec(keyBytes);

        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private String buildSoapEnvelope(String cmsBase64) {
        return """
        <soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wsaa=\"http://wsaa.view.sua.dvadac.desein.afip.gov\">
            <soapenv:Header/>
            <soapenv:Body>
                <wsaa:loginCms>
                    <wsaa:in0>%s</wsaa:in0>
                </wsaa:loginCms>
            </soapenv:Body>
        </soapenv:Envelope>
        """.formatted(cmsBase64);
    }

    private String callWsaaHttp(String soapEnvelope) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wsaaUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "text/xml; charset=UTF-8")
                .header("SOAPAction", "")
                .POST(HttpRequest.BodyPublishers.ofString(soapEnvelope))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200)
            throw new RuntimeException("Respuesta HTTP inválida de WSAA [Status " + response.statusCode() + "]: " + response.body());

        return response.body();
    }

    private AccessTicket parseWsaaResponse(String soapXml) throws Exception {
        DocumentBuilderFactory  factory = DocumentBuilderFactory.newInstance();
        Document                doc     = factory.newDocumentBuilder().parse(new InputSource(new StringReader(soapXml)));

        // loginCmsReturn contiene el XML interno del Ticket de Acceso escapado
        String loginCmsReturn = doc.getElementsByTagName("loginCmsReturn").item(0).getTextContent();

        Document    taDoc           = factory.newDocumentBuilder().parse(new InputSource(new StringReader(loginCmsReturn)));
        String      token           = taDoc.getElementsByTagName("token").item(0).getTextContent();
        String      sign            = taDoc.getElementsByTagName("sign").item(0).getTextContent();
        String      expirationStr   = taDoc.getElementsByTagName("expirationTime").item(0).getTextContent();

        return AccessTicket.builder()
                .token(token)
                .sign(sign)
                .expiration(Instant.parse(expirationStr))
                .build();
    }
}