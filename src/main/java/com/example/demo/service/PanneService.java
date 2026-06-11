package com.example.demo.service;

import com.example.demo.domain.Equipement;
import com.example.demo.domain.Panne;
import com.example.demo.domain.StatutEquipement;
import com.example.demo.domain.StatutPanne;
import com.example.demo.domain.Utilisateur;
import com.example.demo.repository.EquipementRepository;
import com.example.demo.repository.PanneRepository;
import com.example.demo.repository.UtilisateurRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PanneService {
    private final PanneRepository pannes;
    private final EquipementRepository equipements;
    private final UtilisateurRepository utilisateurs;
    private final EquipementService equipementService;

    public PanneService(PanneRepository pannes, EquipementRepository equipements,
            UtilisateurRepository utilisateurs, EquipementService equipementService) {
        this.pannes = pannes;
        this.equipements = equipements;
        this.utilisateurs = utilisateurs;
        this.equipementService = equipementService;
    }

    @Transactional
    public Panne declarer(Panne panne, Long equipementId, Long declarantId) {
        Equipement equipement = equipements.findById(equipementId)
                .orElseThrow(() -> new NotFoundException("Equipement introuvable: " + equipementId));
        panne.setEquipement(equipement);
        panne.setDeclarant(findUtilisateurOrNull(declarantId));
        panne.setStatut(StatutPanne.DECLAREE);
        panne.setDateDeclaration(LocalDateTime.now());
        equipement.setStatut(StatutEquipement.EN_PANNE);
        equipements.save(equipement);
        Panne saved = pannes.save(panne);
        equipementService.ajouterHistorique(equipement, "PANNE_DECLAREE", panne.getDescription());
        return saved;
    }

    @Transactional
    public Panne affecter(Long panneId, Long technicienId) {
        Panne panne = get(panneId);
        Utilisateur technicien = utilisateurs.findById(technicienId)
                .orElseThrow(() -> new NotFoundException("Technicien introuvable: " + technicienId));
        panne.setTechnicien(technicien);
        panne.setStatut(StatutPanne.EN_COURS);
        equipementService.ajouterHistorique(panne.getEquipement(), "PANNE_AFFECTEE", "Technicien: " + technicien.getLogin());
        return pannes.save(panne);
    }

    @Transactional
    public Panne changerStatut(Long panneId, StatutPanne statut) {
        Panne panne = get(panneId);
        panne.setStatut(statut);
        if (statut == StatutPanne.CLOTUREE) {
            panne.setDateCloture(LocalDateTime.now());
            panne.getEquipement().setStatut(StatutEquipement.DISPONIBLE);
            equipements.save(panne.getEquipement());
        }
        equipementService.ajouterHistorique(panne.getEquipement(), "STATUT_PANNE", statut.name());
        return pannes.save(panne);
    }

    private Panne get(Long id) {
        return pannes.findById(id).orElseThrow(() -> new NotFoundException("Panne introuvable: " + id));
    }

    private Utilisateur findUtilisateurOrNull(Long id) {
        return id == null ? null : utilisateurs.findById(id).orElse(null);
    }
}
