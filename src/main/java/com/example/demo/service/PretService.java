package com.example.demo.service;

import com.example.demo.domain.Equipement;
import com.example.demo.domain.Employe;
import com.example.demo.domain.Pret;
import com.example.demo.domain.StatutEquipement;
import com.example.demo.domain.StatutPret;
import com.example.demo.repository.EmployeRepository;
import com.example.demo.repository.EquipementRepository;
import com.example.demo.repository.PretRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PretService {
    private final PretRepository prets;
    private final EquipementRepository equipements;
    private final EmployeRepository employes;
    private final EquipementService equipementService;

    public PretService(PretRepository prets, EquipementRepository equipements,
            EmployeRepository employes, EquipementService equipementService) {
        this.prets = prets;
        this.equipements = equipements;
        this.employes = employes;
        this.equipementService = equipementService;
    }

    @Transactional
    public Pret creer(Long equipementId, Long employeId, LocalDate dateRetourPrevue, String motif) {
        Equipement equipement = equipements.findById(equipementId)
                .orElseThrow(() -> new NotFoundException("Equipement introuvable: " + equipementId));
        Employe employe = employes.findById(employeId)
                .orElseThrow(() -> new NotFoundException("Employe introuvable: " + employeId));
        Pret pret = new Pret();
        pret.setEquipement(equipement);
        pret.setEmploye(employe);
        pret.setDateDepart(LocalDate.now());
        pret.setDateRetourPrevue(dateRetourPrevue);
        pret.setMotif(motif);
        pret.setStatut(StatutPret.EN_ATTENTE);
        equipementService.ajouterHistorique(equipement, "PRET_DEMANDE", employe.getNom() + " " + employe.getPrenom());
        return prets.save(pret);
    }

    @Transactional
    public Pret valider(Long id) {
        Pret pret = prets.findById(id).orElseThrow(() -> new NotFoundException("Pret introuvable: " + id));
        if (pret.getStatut() != StatutPret.EN_ATTENTE) {
            throw new IllegalArgumentException("Seuls les prets en attente peuvent etre valides");
        }
        Equipement equipement = pret.getEquipement();
        if (equipement.getStatut() != StatutEquipement.DISPONIBLE) {
            throw new IllegalArgumentException("Equipement indisponible pour le pret");
        }
        pret.setStatut(StatutPret.VALIDE);
        equipement.setStatut(StatutEquipement.EN_PRET);
        equipements.save(equipement);
        equipementService.ajouterHistorique(equipement, "PRET_VALIDE", pret.getEmploye().getNom() + " " + pret.getEmploye().getPrenom());
        return prets.save(pret);
    }

    @Transactional
    public Pret refuser(Long id) {
        Pret pret = prets.findById(id).orElseThrow(() -> new NotFoundException("Pret introuvable: " + id));
        if (pret.getStatut() != StatutPret.EN_ATTENTE) {
            throw new IllegalArgumentException("Seuls les prets en attente peuvent etre refuses");
        }
        pret.setStatut(StatutPret.REFUSE);
        equipementService.ajouterHistorique(pret.getEquipement(), "PRET_REFUSE", "Demande refusee");
        return prets.save(pret);
    }

    @Transactional
    public Pret cloturer(Long id) {
        Pret pret = prets.findById(id).orElseThrow(() -> new NotFoundException("Pret introuvable: " + id));
        if (pret.getStatut() != StatutPret.VALIDE && pret.getStatut() != StatutPret.EN_RETARD) {
            throw new IllegalArgumentException("Seuls les prets valides ou en retard peuvent etre clotures");
        }
        pret.setDateRetourReelle(LocalDate.now());
        pret.setStatut(StatutPret.CLOTURE);
        pret.getEquipement().setStatut(StatutEquipement.DISPONIBLE);
        equipements.save(pret.getEquipement());
        equipementService.ajouterHistorique(pret.getEquipement(), "PRET_CLOTURE", "Retour valide");
        return prets.save(pret);
    }
}
