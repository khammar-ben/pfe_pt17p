package com.example.demo.service;

import com.example.demo.domain.AuditLog;
import com.example.demo.repository.AuditLogRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final AuditLogRepository auditLogRepository;
    private final boolean mailEnabled;
    private final String fromAddress;
    private final String fromName;

    public NotificationService(ObjectProvider<JavaMailSender> mailSender, AuditLogRepository auditLogRepository,
            @Value("${app.mail.enabled:false}") boolean mailEnabled,
            @Value("${app.mail.from-address:}") String fromAddress,
            @Value("${app.mail.from-name:PT17 Support}") String fromName) {
        this.mailSender = mailSender;
        this.auditLogRepository = auditLogRepository;
        this.mailEnabled = mailEnabled;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    public boolean envoyer(String destinataire, String sujet, String message) {
        String email = destinataire == null ? "" : destinataire.trim();
        if (email.isBlank()) {
            journaliser("NOTIFICATION_IGNORE", "Destinataire vide: " + sujet);
            return false;
        }
        if (!mailEnabled) {
            LOGGER.info("Notification email desactivee: {} -> {}", email, sujet);
            journaliser("NOTIFICATION_EMAIL_DESACTIVEE", email + " | " + sujet);
            return false;
        }

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            LOGGER.warn("Notification non envoyee, SMTP non configure: {} -> {}", email, sujet);
            journaliser("NOTIFICATION_NON_ENVOYEE", email + " | " + sujet);
            return false;
        }

        try {
            MimeMessage mail = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, false, "UTF-8");
            if (!fromAddress.isBlank()) {
                if (fromName.isBlank()) {
                    helper.setFrom(fromAddress);
                } else {
                    helper.setFrom(fromAddress, fromName);
                }
            }
            helper.setTo(email);
            helper.setSubject(sujet);
            helper.setText(message, false);
            sender.send(mail);
            journaliser("NOTIFICATION_ENVOYEE", email + " | " + sujet);
            return true;
        } catch (Exception exception) {
            LOGGER.error("Echec SMTP vers {} pour {}", email, sujet, exception);
            journaliser("NOTIFICATION_ECHEC_SMTP",
                    email + " | " + sujet + " | " + exception.getClass().getSimpleName()
                            + " | " + exception.getMessage());
            return false;
        }
    }

    private void journaliser(String action, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setDetails(details);
        auditLogRepository.save(auditLog);
    }
}
