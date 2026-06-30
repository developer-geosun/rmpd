package com.geosun.rmpd.application.service;

import com.geosun.rmpd.domain.model.Declaration;
import java.util.Locale;
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
    private final String locale;

    public RegistrationEmailService(
            @org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender mailSender,
            @Value("${rmpd.mail.enabled:false}") boolean enabled,
            @Value("${rmpd.mail.from:noreply@rmpd.local}") String fromAddress,
            @Value("${rmpd.mail.locale:uk}") String locale) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
        this.locale = locale != null ? locale.toLowerCase(Locale.ROOT) : "uk";
    }

    public void sendRegistrationNotification(Declaration declaration) {
        String recipient = declaration.getCarrier().getEmail();
        MailContent content = resolveContent(declaration);

        if (!enabled || mailSender == null) {
            log.info("Email (disabled): to={} subject={}", recipient, content.subject());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(content.subject());
        message.setText(content.body());
        mailSender.send(message);
        log.info("Registration email sent to {}", recipient);
    }

    private MailContent resolveContent(Declaration declaration) {
        String ref = declaration.getReferenceNumber() != null ? declaration.getReferenceNumber() : "—";
        String sysRef = declaration.getPuescSysRef() != null ? declaration.getPuescSysRef() : "—";

        return switch (locale) {
            case "pl" -> new MailContent(
                    "RMPD100 zarejestrowane: " + ref,
                    """
                    Deklaracja RMPD100 została pomyślnie zarejestrowana w PUESC.

                    Numer referencyjny: %s
                    sysRef: %s
                    """.formatted(ref, sysRef));
            case "en" -> new MailContent(
                    "RMPD100 registered: " + ref,
                    """
                    RMPD100 declaration has been successfully registered in PUESC.

                    Reference number: %s
                    sysRef: %s
                    """.formatted(ref, sysRef));
            default -> new MailContent(
                    "RMPD100 зареєстровано: " + ref,
                    """
                    Декларація RMPD100 успішно зареєстрована в PUESC.

                    Референсний номер: %s
                    sysRef: %s
                    """.formatted(ref, sysRef));
        };
    }

    private record MailContent(String subject, String body) {}
}
