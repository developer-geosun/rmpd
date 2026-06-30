package com.geosun.rmpd.application.service;

import com.geosun.rmpd.application.dto.PuescConnectionTestDto;
import com.geosun.rmpd.application.dto.PuescCredentialDto;
import com.geosun.rmpd.application.dto.PuescCredentialUpsertDto;
import com.geosun.rmpd.application.exception.ResourceNotFoundException;
import com.geosun.rmpd.domain.enums.PuescEnvironment;
import com.geosun.rmpd.domain.model.Carrier;
import com.geosun.rmpd.domain.model.PuescCredential;
import com.geosun.rmpd.infrastructure.crypto.AesGcmCipher;
import com.geosun.rmpd.infrastructure.persistence.CarrierRepository;
import com.geosun.rmpd.infrastructure.persistence.PuescCredentialRepository;
import com.geosun.rmpd.infrastructure.puesc.ConnectionTestResult;
import com.geosun.rmpd.infrastructure.puesc.PuescSoapClient;
import com.geosun.rmpd.infrastructure.security.SecurityUtils;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PuescCredentialService {

    private final PuescCredentialRepository credentialRepository;
    private final CarrierRepository carrierRepository;
    private final AesGcmCipher aesGcmCipher;
    private final PuescSoapClient puescSoapClient;
    private final PuescEnvironment defaultEnvironment;

    public PuescCredentialService(
            PuescCredentialRepository credentialRepository,
            CarrierRepository carrierRepository,
            AesGcmCipher aesGcmCipher,
            PuescSoapClient puescSoapClient,
            @Value("${puesc.env:test}") String env) {
        this.credentialRepository = credentialRepository;
        this.carrierRepository = carrierRepository;
        this.aesGcmCipher = aesGcmCipher;
        this.puescSoapClient = puescSoapClient;
        this.defaultEnvironment = "prod".equalsIgnoreCase(env) ? PuescEnvironment.PROD : PuescEnvironment.TEST;
    }

    @Transactional(readOnly = true)
    public PuescCredentialDto get() {
        return credentialRepository.findByCarrierIdAndEnvironment(SecurityUtils.requireCarrierId(), defaultEnvironment)
                .map(this::toDto)
                .orElse(new PuescCredentialDto(defaultEnvironment, "", false, null, null, null, null, false, null, null));
    }

    @Transactional
    public PuescCredentialDto upsert(PuescCredentialUpsertDto dto) {
        Long carrierId = SecurityUtils.requireCarrierId();
        Carrier carrier = carrierRepository.findById(carrierId)
                .orElseThrow(() -> new ResourceNotFoundException("Перевізника не знайдено"));
        PuescEnvironment env = dto.environment() != null ? dto.environment() : defaultEnvironment;

        PuescCredential credential = credentialRepository.findByCarrierIdAndEnvironment(carrierId, env)
                .orElseGet(() -> {
                    PuescCredential created = new PuescCredential();
                    created.setCarrier(carrier);
                    created.setEnvironment(env);
                    return created;
                });

        credential.setUsername(dto.username());
        credential.setSigningCertPath(dto.signingCertPath());
        credential.setIdSiscRop(blankToNull(dto.idSiscRop()));
        credential.setIdSiscRof(blankToNull(dto.idSiscRof()));
        credential.setIdSiscP(blankToNull(dto.idSiscP()));
        credential.setActive(true);

        if (dto.password() != null && !dto.password().isBlank()) {
            AesGcmCipher.EncryptedPayload encrypted = aesGcmCipher.encrypt(dto.password());
            credential.setPasswordEncrypted(encrypted.ciphertext());
            credential.setPasswordIv(encrypted.iv());
        } else if (credential.getPasswordEncrypted() == null) {
            throw new IllegalArgumentException("Пароль PUESC обов'язковий при першому збереженні");
        }

        return toDto(credentialRepository.save(credential));
    }

    @Transactional
    public PuescConnectionTestDto testConnection() {
        PuescCredential credential = requireCredential();
        String password = decryptPassword(credential);
        ConnectionTestResult result = puescSoapClient.testConnection(credential.getUsername(), password);
        credential.setLastTestAt(Instant.now());
        credential.setLastTestOk(result.success());
        credentialRepository.save(credential);
        return new PuescConnectionTestDto(result.success(), result.message(), result.mock());
    }

    @Transactional(readOnly = true)
    public PuescCredential requireActiveCredential() {
        return credentialRepository.findActiveByCarrierId(SecurityUtils.requireCarrierId())
                .orElseThrow(() -> new ResourceNotFoundException("Налаштування PUESC не знайдено"));
    }

    public String decryptPassword(PuescCredential credential) {
        return aesGcmCipher.decrypt(credential.getPasswordEncrypted(), credential.getPasswordIv());
    }

    private PuescCredential requireCredential() {
        return credentialRepository.findByCarrierIdAndEnvironment(SecurityUtils.requireCarrierId(), defaultEnvironment)
                .or(() -> credentialRepository.findActiveByCarrierId(SecurityUtils.requireCarrierId()))
                .orElseThrow(() -> new ResourceNotFoundException("Налаштування PUESC не знайдено"));
    }

    private PuescCredentialDto toDto(PuescCredential credential) {
        return new PuescCredentialDto(
                credential.getEnvironment(),
                credential.getUsername(),
                credential.getPasswordEncrypted() != null,
                credential.getSigningCertPath(),
                credential.getIdSiscRop(),
                credential.getIdSiscRof(),
                credential.getIdSiscP(),
                credential.isActive(),
                credential.getLastTestAt(),
                credential.getLastTestOk());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
