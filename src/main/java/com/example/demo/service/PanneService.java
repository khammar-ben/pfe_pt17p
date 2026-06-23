package com.example.demo.service;

import com.example.demo.domain.Equipement;
import com.example.demo.domain.Panne;
import com.example.demo.domain.Piece;
import com.example.demo.domain.RoleType;
import com.example.demo.domain.StatutEquipement;
import com.example.demo.domain.StatutPanne;
import com.example.demo.domain.Utilisateur;
import com.example.demo.repository.EquipementRepository;
import com.example.demo.repository.PanneRepository;
import com.example.demo.repository.PieceRepository;
import com.example.demo.repository.UtilisateurRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PanneService {
    private final PanneRepository pannes;
    private final EquipementRepository equipements;
    private final UtilisateurRepository utilisateurs;
    private final PieceRepository pieces;
    private final EquipementService equipementService;
    private final NotificationInterneService notificationInterneService;

    public PanneService(PanneRepository pannes, EquipementRepository equipements,
            UtilisateurRepository utilisateurs, PieceRepository pieces, EquipementService equipementService,
            NotificationInterneService notificationInterneService) {
        this.pannes = pannes;
        this.equipements = equipements;
        this.utilisateurs = utilisateurs;
        this.pieces = pieces;
        this.equipementService = equipementService;
        this.notificationInterneService = notificationInterneService;
    }

    @Transactional
    public Panne declarer(Panne panne, Long equipementId, Long declarantId) {
        Equipement equipement = equipements.findById(equipementId)
                .orElseThrow(() -> new NotFoundException("Equipement introuvable: " + equipementId));
        Utilisateur declarant = findUtilisateurOrNull(declarantId);
        if (declarant == null || declarant.getEmploye() == null) {
            throw new IllegalArgumentException("Le declarant doit etre lie a un employe");
        }
        if (declarant.getRole() == RoleType.EMPLOYE
                && (equipement.getEmploye() == null
                || !equipement.getEmploye().getId().equals(declarant.getEmploye().getId()))) {
            throw new IllegalArgumentException("Vous ne pouvez declarer une panne que sur votre equipement");
        }
        if (equipement.getStatut() == StatutEquipement.EN_PANNE) {
            throw new IllegalArgumentException("Equipement deja en panne: " + equipement.getNumSerie());
        }
        panne.setEquipement(equipement);
        panne.setDeclarant(declarant);
        panne.setStatut(StatutPanne.DECLAREE);
        panne.setDateDeclaration(LocalDateTime.now());
        equipement.setStatut(StatutEquipement.EN_PANNE);
        equipements.save(equipement);
        Panne saved = pannes.save(panne);
        equipementService.ajouterHistorique(equipement, "PANNE_DECLAREE", panne.getDescription());
        notificationInterneService.notifierAdmins(
                "Nouvelle panne declaree",
                "Panne #" + saved.getId() + " - " + equipement.getNumSerie() + " : " + panne.getDescription(),
                "PANNE_DECLAREE",
                saved.getId());
        return saved;
    }

    @Transactional
    public Panne declarerDepuisStock(Panne panne, Long pieceId, Long declarantId) {
        Piece piece = pieces.findById(pieceId)
                .orElseThrow(() -> new NotFoundException("Piece introuvable: " + pieceId));
        if (piece.getQuantiteStock() <= 0) {
            throw new IllegalArgumentException("Stock indisponible pour cette piece");
        }
        Utilisateur declarant = utilisateurs.findById(declarantId)
                .orElseThrow(() -> new NotFoundException("Declarant introuvable: " + declarantId));
        if (declarant.getEmploye() == null) {
            throw new IllegalArgumentException("Le declarant doit etre lie a un employe");
        }

        piece.setQuantiteStock(piece.getQuantiteStock() - 1);
        pieces.save(piece);

        Equipement equipement = new Equipement();
        equipement.setNumSerie(nextNumSerie(piece.getReference()));
        equipement.setType(piece.getDesignation());
        equipement.setMarque(piece.getFournisseur() == null ? null : piece.getFournisseur().getNom());
        equipement.setValeur(piece.getPrixUnitaire());
        equipement.setEmploye(declarant.getEmploye());
        equipement.setService(declarant.getEmploye().getService());
        equipement.setStatut(StatutEquipement.AFFECTE);
        equipements.save(equipement);
        equipementService.ajouterHistorique(equipement, "EQUIPEMENT_AFFECTE",
                declarant.getEmploye().getNom() + " " + declarant.getEmploye().getPrenom());

        return declarer(panne, equipement.getId(), declarantId);
    }

    @Transactional
    public Panne affecter(Long panneId, Long technicienId) {
        Panne panne = get(panneId);
        if (panne.getStatut() == StatutPanne.REPAREE || panne.getStatut() == StatutPanne.CLOTUREE) {
            throw new IllegalArgumentException("Cette panne est deja terminee");
        }
        Utilisateur technicien = utilisateurs.findById(technicienId)
                .orElseThrow(() -> new NotFoundException("Technicien introuvable: " + technicienId));
        if (technicien.getRole() != RoleType.TECHNICIEN) {
            throw new IllegalArgumentException("L'utilisateur selectionne n'est pas un technicien");
        }
        panne.setTechnicien(technicien);
        panne.setStatut(StatutPanne.EN_COURS);
        equipementService.ajouterHistorique(panne.getEquipement(), "PANNE_AFFECTEE", "Technicien: " + technicien.getLogin());
        Panne saved = pannes.save(panne);
        notificationInterneService.affecterTache("PANNE_TACHE", panneId, technicien);
        return saved;
    }

    @Transactional
    public Panne publier(Long panneId) {
        Panne panne = pannes.findByIdForUpdate(panneId)
                .orElseThrow(() -> new NotFoundException("Panne introuvable: " + panneId));
        if (panne.getTechnicien() != null || panne.getStatut() != StatutPanne.DECLAREE) {
            throw new IllegalArgumentException("Cette panne ne peut plus etre envoyee aux techniciens");
        }
        panne.setStatut(StatutPanne.A_AFFECTER);
        Panne saved = pannes.save(panne);
        notificationInterneService.notifierTechniciens(
                "Nouvelle tache disponible",
                "Panne #" + panne.getId() + " - " + panne.getDescription(),
                "PANNE_TACHE",
                panne.getId());
        return saved;
    }

    @Transactional
    public Panne claim(Long panneId, String technicienLogin) {
        Panne panne = pannes.findByIdForUpdate(panneId)
                .orElseThrow(() -> new NotFoundException("Panne introuvable: " + panneId));
        Utilisateur technicien = utilisateurs.findByLogin(technicienLogin)
                .orElseThrow(() -> new NotFoundException("Technicien introuvable: " + technicienLogin));
        if (technicien.getRole() != RoleType.TECHNICIEN) {
            throw new IllegalArgumentException("Action reservee au technicien");
        }
        if (panne.getTechnicien() != null || panne.getStatut() != StatutPanne.A_AFFECTER) {
            throw new IllegalArgumentException("Cette tache a deja ete prise par un autre technicien");
        }
        panne.setTechnicien(technicien);
        panne.setStatut(StatutPanne.EN_COURS);
        Panne saved = pannes.save(panne);
        notificationInterneService.affecterTache("PANNE_TACHE", panneId, technicien);
        notificationInterneService.notifierAdmins(
                "Tache affectee",
                "La panne #" + panneId + " a ete prise par " + technicien.getLogin(),
                "PANNE_AFFECTEE",
                panneId);
        equipementService.ajouterHistorique(
                panne.getEquipement(), "PANNE_AFFECTEE", "Technicien: " + technicien.getLogin());
        return saved;
    }

    @Transactional
    public Panne changerStatut(Long panneId, StatutPanne statut, String login, boolean admin) {
        Panne panne = get(panneId);
        if (!admin && (panne.getTechnicien() == null || !panne.getTechnicien().getLogin().equals(login))) {
            throw new IllegalArgumentException("Cette tache n'est pas affectee a ce technicien");
        }
        if (statut == StatutPanne.DECLAREE || statut == StatutPanne.A_AFFECTER) {
            throw new IllegalArgumentException("Transition de statut non autorisee");
        }
        if (statut == StatutPanne.CLOTUREE && (!admin || panne.getStatut() != StatutPanne.REPAREE)) {
            throw new IllegalArgumentException("Seul l'admin peut cloturer une panne reparee");
        }
        if (statut == StatutPanne.REPAREE && panne.getTechnicien() == null) {
            throw new IllegalArgumentException("La panne doit etre affectee avant d'etre reparee");
        }
        panne.setStatut(statut);
        if (statut == StatutPanne.CLOTUREE) {
            panne.setDateCloture(LocalDateTime.now());
            panne.getEquipement().setStatut(StatutEquipement.DISPONIBLE);
            equipements.save(panne.getEquipement());
            notificationInterneService.notifierAdmins(
                    "Panne cloturee",
                    "La panne #" + panneId + " a ete cloturee",
                    "PANNE_CLOTUREE",
                    panneId);
            notificationInterneService.notifierUtilisateur(
                    panne.getDeclarant(),
                    "Panne cloturee",
                    "Votre panne #" + panneId + " est cloturee",
                    "PANNE_CLOTUREE",
                    panneId);
        }
        if (statut == StatutPanne.REPAREE || statut == StatutPanne.CLOTUREE) {
            notificationInterneService.terminer("PANNE_TACHE", panneId);
        }
        equipementService.ajouterHistorique(panne.getEquipement(), "STATUT_PANNE", statut.name());
        return pannes.save(panne);
    }

    @Transactional
    public Panne changerStatut(Long panneId, StatutPanne statut) {
        return changerStatut(panneId, statut, "systeme", true);
    }

    private Panne get(Long id) {
        return pannes.findById(id).orElseThrow(() -> new NotFoundException("Panne introuvable: " + id));
    }

    private Utilisateur findUtilisateurOrNull(Long id) {
        return id == null ? null : utilisateurs.findById(id).orElse(null);
    }

    private String nextNumSerie(String reference) {
        String base = reference == null || reference.isBlank()
                ? "EQUIPEMENT"
                : reference.replaceAll("[^A-Za-z0-9-]", "-");
        String numSerie = base + "-" + System.currentTimeMillis();
        while (equipements.findByNumSerie(numSerie).isPresent()) {
            numSerie = base + "-" + System.nanoTime();
        }
        return numSerie;
    }
}
