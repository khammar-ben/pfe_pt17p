package com.example.demo.service;

import com.example.demo.domain.AuditLog;
import com.example.demo.repository.AuditLogRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final AuditLogRepository auditLogRepository;

    public NotificationService(ObjectProvider<JavaMailSender> mailSender, AuditLogRepository auditLogRepository) {
        this.mailSender = mailSender;
        this.auditLogRepository = auditLogRepository;
    }

    public boolean envoyer(String destinataire, String sujet, String message) {
        if (destinataire == null || destinataire.isBlank()) {
            journaliser("NOTIFICATION_IGNORE", "Destinataire vide: " + sujet);
            return false;
        }

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            LOGGER.warn("Notification non envoyee, SMTP non configure: {} -> {}", destinataire, sujet);
            journaliser("NOTIFICATION_NON_ENVOYEE", destinataire + " | " + sujet);
            return false;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(destinataire);
        mail.setSubject(sujet);
        mail.setText(message);
        sender.send(mail);
        journaliser("NOTIFICATION_ENVOYEE", destinataire + " | " + sujet);
        return true;
    }

    private void journaliser(String action, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setDetails(details);
        auditLogRepository.save(auditLog);
    }
}
