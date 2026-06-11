package com.example.demo.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "pannes")
public class Panne {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String description;

    @Enumerated(EnumType.STRING)
    private NiveauUrgence urgence = NiveauUrgence.MOYENNE;

    @Enumerated(EnumType.STRING)
    private StatutPanne statut = StatutPanne.DECLAREE;

    private LocalDateTime dateDeclaration = LocalDateTime.now();
    private LocalDateTime dateCloture;
    private String photoPath;

    @NotNull
    @ManyToOne
    private Equipement equipement;

    @ManyToOne
    private Utilisateur declarant;

    @ManyToOne
    private Utilisateur technicien;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public NiveauUrgence getUrgence() {
        return urgence;
    }

    public void setUrgence(NiveauUrgence urgence) {
        this.urgence = urgence;
    }

    public StatutPanne getStatut() {
        return statut;
    }

    public void setStatut(StatutPanne statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateDeclaration() {
        return dateDeclaration;
    }

    public void setDateDeclaration(LocalDateTime dateDeclaration) {
        this.dateDeclaration = dateDeclaration;
    }

    public LocalDateTime getDateCloture() {
        return dateCloture;
    }

    public void setDateCloture(LocalDateTime dateCloture) {
        this.dateCloture = dateCloture;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public Equipement getEquipement() {
        return equipement;
    }

    public void setEquipement(Equipement equipement) {
        this.equipement = equipement;
    }

    public Utilisateur getDeclarant() {
        return declarant;
    }

    public void setDeclarant(Utilisateur declarant) {
        this.declarant = declarant;
    }

    public Utilisateur getTechnicien() {
        return technicien;
    }

    public void setTechnicien(Utilisateur technicien) {
        this.technicien = technicien;
    }
}
