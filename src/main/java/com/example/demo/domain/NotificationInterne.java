package com.example.demo.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications_internes")
public class NotificationInterne {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;
    private String message;
    private String type;
    private Long referenceId;
    private LocalDateTime dateCreation = LocalDateTime.now();
    private boolean lu;

    @Enumerated(EnumType.STRING)
    private StatutNotification statut = StatutNotification.NOUVELLE;

    @ManyToOne
    private Utilisateur destinataire;

    public Long getId() { return id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public boolean isLu() { return lu; }
    public void setLu(boolean lu) { this.lu = lu; }
    public StatutNotification getStatut() { return statut; }
    public void setStatut(StatutNotification statut) { this.statut = statut; }
    public Utilisateur getDestinataire() { return destinataire; }
    public void setDestinataire(Utilisateur destinataire) { this.destinataire = destinataire; }
}
