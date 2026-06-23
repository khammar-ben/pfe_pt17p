package com.example.demo.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Entity
@Table(name = "pieces")
public class Piece {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String reference;

    @NotBlank
    private String designation;

    private int quantiteStock;
    private int seuilMinimum;
    private String localisation;
    private BigDecimal prixUnitaire = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie_usage")
    private UsagePiece usage;

    @ManyToOne
    private Fournisseur fournisseur;

    public boolean stockCritique() {
        return quantiteStock < seuilMinimum;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public int getQuantiteStock() {
        return quantiteStock;
    }

    public void setQuantiteStock(int quantiteStock) {
        this.quantiteStock = quantiteStock;
    }

    public int getSeuilMinimum() {
        return seuilMinimum;
    }

    public void setSeuilMinimum(int seuilMinimum) {
        this.seuilMinimum = seuilMinimum;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public Fournisseur getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(Fournisseur fournisseur) {
        this.fournisseur = fournisseur;
    }

    public UsagePiece getUsage() {
        if (usage != null) {
            return usage;
        }
        String label = ((reference == null ? "" : reference) + " "
                + (designation == null ? "" : designation)).toUpperCase();
        return label.contains("PC-") || label.contains("ECRAN")
                || label.contains("CLAVIER") || label.contains("SOURIS")
                ? UsagePiece.MATERIEL
                : UsagePiece.RECHANGE;
    }

    public void setUsage(UsagePiece usage) {
        this.usage = usage;
    }
}
