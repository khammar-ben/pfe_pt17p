package com.example.demo.service;

import com.example.demo.domain.Equipement;
import com.example.demo.domain.HistoriqueEquipement;
import com.example.demo.repository.EquipementRepository;
import com.example.demo.repository.HistoriqueEquipementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipementService {
    private final EquipementRepository equipements;
    private final HistoriqueEquipementRepository historiques;

    public EquipementService(EquipementRepository equipements, HistoriqueEquipementRepository historiques) {
        this.equipements = equipements;
        this.historiques = historiques;
    }

    @Transactional
    public Equipement save(Equipement equipement) {
        Equipement saved = equipements.save(equipement);
        ajouterHistorique(saved, "EQUIPEMENT_ENREGISTRE", "Creation ou mise a jour de la fiche equipement");
        return saved;
    }

    @Transactional
    public void ajouterHistorique(Equipement equipement, String evenement, String details) {
        HistoriqueEquipement historique = new HistoriqueEquipement();
        historique.setEquipement(equipement);
        historique.setEvenement(evenement);
        historique.setDetails(details);
        historiques.save(historique);
    }
}
