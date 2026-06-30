package com.geosun.rmpd.application.service;

import com.geosun.rmpd.domain.model.Declaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class RegistrationEmailService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationEmailService.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String fromAddress;

    public RegistrationEmailService(
            @org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender mailSender,
            @Value("${rmpd.mail.enabled:false}") boolean enabled,
            @Value("${rmpd.mail.from:noreply@rmpd.local}") String fromAddress) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
    }

    public void sendRegistrationNotification(Declaration declaration) {
        String recipient = declaration.getCarrier().getEmail();
        String subject = "RMPD100 зареєстровано: " + declaration.getReferenceNumber();
        String body = """
                Декларація RMPD100 успішно зареєстрована в PUESC.

                Референсний номер: %s
                sysRef: %s
                """.formatted(declaration.getReferenceNumber(), declaration.getPuescSysRef());

        if (!enabled || mailSender == null) {
            log.info("Email (disabled): to={} subject={}", recipient, subject);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("Registration email sent to {}", recipient);
    }
}
