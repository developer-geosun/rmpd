package com.geosun.rmpd.infrastructure.xml;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.signature.XAdESService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore.PasswordProtection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class XmlSigningService {

    private static final Logger log = LoggerFactory.getLogger(XmlSigningService.class);

    private final String defaultCertPassword;

    public XmlSigningService(@Value("${rmpd.signing.cert-password:}") String defaultCertPassword) {
        this.defaultCertPassword = defaultCertPassword;
    }

    /**
     * Підпис XAdES-BES (baseline-B) enveloped. Без сертифіката — повертає XML без змін (dev/mock).
     */
    public byte[] sign(byte[] xmlContent, String signingCertPath) {
        if (signingCertPath == null || signingCertPath.isBlank()) {
            log.debug("Signing skipped: signingCertPath not configured");
            return xmlContent;
        }
        Path certPath = Path.of(signingCertPath);
        if (!Files.isRegularFile(certPath)) {
            log.warn("Signing skipped: certificate file not found at {}", signingCertPath);
            return xmlContent;
        }
        if (defaultCertPassword == null || defaultCertPassword.isBlank()) {
            log.warn("Signing skipped: RMPD_SIGNING_CERT_PASSWORD not set");
            return xmlContent;
        }
        try {
            return signWithPkcs12(xmlContent, certPath, defaultCertPassword);
        } catch (Exception ex) {
            throw new IllegalStateException("Помилка XAdES підпису: " + ex.getMessage(), ex);
        }
    }

    private byte[] signWithPkcs12(byte[] xmlContent, Path certPath, String password) throws IOException {
        try (Pkcs12SignatureToken token =
                new Pkcs12SignatureToken(certPath.toString(), new PasswordProtection(password.toCharArray()))) {
            DSSPrivateKeyEntry privateKey = token.getKeys().getFirst();

            XAdESSignatureParameters parameters = new XAdESSignatureParameters();
            parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_B);
            parameters.setSignaturePackaging(SignaturePackaging.ENVELOPED);
            parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
            parameters.setSigningCertificate(privateKey.getCertificate());
            parameters.setCertificateChain(privateKey.getCertificateChain());

            CommonCertificateVerifier verifier = new CommonCertificateVerifier();
            XAdESService service = new XAdESService(verifier);

            DSSDocument document = new InMemoryDocument(xmlContent);
            ToBeSigned dataToSign = service.getDataToSign(document, parameters);
            SignatureValue signatureValue = token.sign(dataToSign, parameters.getDigestAlgorithm(), privateKey);
            DSSDocument signed = service.signDocument(document, parameters, signatureValue);

            log.info("XML signed with XAdES-BES, cert={}", certPath.getFileName());
            return signed.openStream().readAllBytes();
        }
    }
}
