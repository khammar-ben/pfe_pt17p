package com.example.demo.service;

import com.example.demo.domain.Panne;
import com.example.demo.domain.Reparation;
import com.example.demo.domain.RoleType;
import com.example.demo.domain.StatutPanne;
import com.example.demo.domain.Utilisateur;
import com.example.demo.domain.TypeMouvementStock;
import com.example.demo.repository.PanneRepository;
import com.example.demo.repository.ReparationRepository;
import com.example.demo.repository.UtilisateurRepository;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReparationService {
    private final ReparationRepository reparations;
    private final PanneRepository pannes;
    private final PanneService panneService;
    private final StockService stockService;
    private final UtilisateurRepository utilisateurs;
    private final NotificationInterneService notificationInterneService;

    public ReparationService(ReparationRepository reparations, PanneRepository pannes, PanneService panneService,
            StockService stockService, UtilisateurRepository utilisateurs,
            NotificationInterneService notificationInterneService) {
        this.reparations = reparations;
        this.pannes = pannes;
        this.panneService = panneService;
        this.stockService = stockService;
        this.utilisateurs = utilisateurs;
        this.notificationInterneService = notificationInterneService;
    }

    @Transactional
    public Reparation creer(Reparation reparation, Long panneId) {
        return creer(reparation, panneId, List.of());
    }

    @Transactional
    public Reparation creer(Reparation reparation, Long panneId, List<PieceConsommee> piecesConsommees) {
        return creer(reparation, panneId, null, piecesConsommees);
    }

    @Transactional
    public Reparation creer(Reparation reparation, Long panneId, Long technicienId,
            List<PieceConsommee> piecesConsommees) {
        Panne panne = pannes.findById(panneId).orElseThrow(() -> new NotFoundException("Panne introuvable: " + panneId));
        if (panne.getStatut() == StatutPanne.CLOTUREE || panne.getStatut() == StatutPanne.REPAREE) {
            throw new IllegalArgumentException("Cette panne est deja terminee");
        }
        if (reparations.existsByPanneIdAndDateFinIsNull(panneId)) {
            throw new IllegalArgumentException("Une reparation est deja en cours pour cette panne");
        }
        Utilisateur technicien = technicienId == null ? panne.getTechnicien() : utilisateurs.findById(technicienId)
                .orElseThrow(() -> new NotFoundException("Technicien introuvable: " + technicienId));
        if (technicien == null || technicien.getRole() != RoleType.TECHNICIEN) {
            throw new IllegalArgumentException("Selectionnez un technicien");
        }
        if (panne.getTechnicien() != null && !panne.getTechnicien().getId().equals(technicien.getId())) {
            throw new IllegalArgumentException("Cette panne est deja affectee a un autre technicien");
        }
        panne.setTechnicien(technicien);
        reparation.setPanne(panne);
        reparation.setTechnicien(technicien);
        reparation.setDateDebut(LocalDateTime.now());
        panneService.changerStatut(panneId, StatutPanne.EN_COURS);
        Reparation saved = reparations.save(reparation);
        notificationInterneService.terminer("PANNE_TACHE", panneId);
        notificationInterneService.notifierUtilisateur(
                technicien,
                "Nouvelle reparation affectee",
                "Reparation #" + saved.getId() + " pour la panne #" + panneId,
                "REPARATION_AFFECTEE",
                saved.getId());
        notificationInterneService.notifierUtilisateur(
                panne.getDeclarant(),
                "Panne prise en charge",
                "Votre panne #" + panneId + " est prise en charge",
                "PANNE_EN_COURS",
                panneId);
        for (PieceConsommee piece : piecesConsommees) {
            stockService.enregistrerMouvement(
                    piece.pieceId(),
                    piece.quantite(),
                    TypeMouvementStock.CONSOMMATION,
                    "Consommation reparation #" + saved.getId(),
                    null,
                    saved.getId(),
                    saved.getTechnicien() == null ? null : saved.getTechnicien().getId());
        }
        return saved;
    }

    @Transactional
    public Reparation cloturer(Long id, Integer noteSatisfaction, String login, boolean admin) {
        Reparation reparation = reparations.findById(id)
                .orElseThrow(() -> new NotFoundException("Reparation introuvable: " + id));
        if (reparation.getDateFin() != null) {
            throw new IllegalArgumentException("Cette reparation est deja cloturee");
        }
        if (!admin && (reparation.getTechnicien() == null
                || !reparation.getTechnicien().getLogin().equals(login))) {
            throw new IllegalArgumentException("Cette reparation n'est pas affectee a ce technicien");
        }
        if (noteSatisfaction != null && (noteSatisfaction < 1 || noteSatisfaction > 5)) {
            throw new IllegalArgumentException("La note doit etre comprise entre 1 et 5");
        }
        reparation.setDateFin(LocalDateTime.now());
        reparation.setNoteSatisfaction(noteSatisfaction);
        panneService.changerStatut(reparation.getPanne().getId(), StatutPanne.REPAREE);
        Reparation saved = reparations.save(reparation);
        notificationInterneService.terminer("REPARATION_AFFECTEE", id);
        notificationInterneService.notifierAdmins(
                "Reparation terminee",
                "La reparation #" + id + " a ete terminee par " + login,
                "REPARATION_TERMINEE",
                id);
        notificationInterneService.notifierUtilisateur(
                reparation.getPanne().getDeclarant(),
                "Equipement repare",
                "La reparation de votre panne #" + reparation.getPanne().getId() + " est terminee",
                "REPARATION_TERMINEE",
                id);
        return saved;
    }

    @Transactional
    public Reparation executer(Long id, String diagnostic, BigDecimal coutTotal,
            List<PieceConsommee> piecesConsommees, StatutPanne resultat, String login) {
        Reparation reparation = reparations.findById(id)
                .orElseThrow(() -> new NotFoundException("Reparation introuvable: " + id));
        if (reparation.getDateFin() != null) {
            throw new IllegalArgumentException("Cette reparation est deja terminee");
        }
        if (reparation.getTechnicien() == null
                || !reparation.getTechnicien().getLogin().equals(login)) {
            throw new IllegalArgumentException("Cette reparation n'est pas affectee a ce technicien");
        }
        if (resultat != StatutPanne.REPAREE && resultat != StatutPanne.EN_ATTENTE_PIECE) {
            throw new IllegalArgumentException("Resultat de reparation invalide");
        }
        if (diagnostic != null && !diagnostic.isBlank()) {
            reparation.setDiagnostic(diagnostic.trim());
        }
        if (coutTotal != null) {
            if (coutTotal.signum() < 0) {
                throw new IllegalArgumentException("Le cout ne peut pas etre negatif");
            }
            reparation.setCoutTotal(coutTotal);
        }
        if (resultat == StatutPanne.EN_ATTENTE_PIECE) {
            panneService.changerStatut(
                    reparation.getPanne().getId(), StatutPanne.EN_ATTENTE_PIECE, login, false);
            notificationInterneService.notifierAdmins(
                    "Piece requise",
                    "La reparation #" + id + " est en attente de piece",
                    "REPARATION_ATTENTE_PIECE",
                    id);
            return reparations.save(reparation);
        }
        for (PieceConsommee piece : piecesConsommees == null ? List.<PieceConsommee>of() : piecesConsommees) {
            stockService.enregistrerMouvement(
                    piece.pieceId(),
                    piece.quantite(),
                    TypeMouvementStock.CONSOMMATION,
                    "Consommation reparation #" + id,
                    null,
                    id,
                    reparation.getTechnicien().getId());
        }
        reparation.setDateFin(LocalDateTime.now());
        Reparation saved = reparations.save(reparation);
        panneService.changerStatut(
                reparation.getPanne().getId(), StatutPanne.REPAREE, login, false);
        notificationInterneService.terminer("REPARATION_AFFECTEE", id);
        notificationInterneService.notifierAdmins(
                "Reparation terminee",
                "La reparation #" + id + " a ete terminee par " + login,
                "REPARATION_TERMINEE",
                id);
        notificationInterneService.notifierUtilisateur(
                reparation.getPanne().getDeclarant(),
                "Equipement repare",
                "La reparation de votre panne #" + reparation.getPanne().getId() + " est terminee",
                "REPARATION_TERMINEE",
                id);
        return saved;
    }

    public record PieceConsommee(Long pieceId, int quantite) {
    }
}
