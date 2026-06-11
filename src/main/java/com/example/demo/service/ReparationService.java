package com.example.demo.service;

import com.example.demo.domain.Panne;
import com.example.demo.domain.Reparation;
import com.example.demo.domain.StatutPanne;
import com.example.demo.domain.TypeMouvementStock;
import com.example.demo.repository.PanneRepository;
import com.example.demo.repository.ReparationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReparationService {
    private final ReparationRepository reparations;
    private final PanneRepository pannes;
    private final PanneService panneService;
    private final StockService stockService;

    public ReparationService(ReparationRepository reparations, PanneRepository pannes, PanneService panneService,
            StockService stockService) {
        this.reparations = reparations;
        this.pannes = pannes;
        this.panneService = panneService;
        this.stockService = stockService;
    }

    @Transactional
    public Reparation creer(Reparation reparation, Long panneId) {
        return creer(reparation, panneId, List.of());
    }

    @Transactional
    public Reparation creer(Reparation reparation, Long panneId, List<PieceConsommee> piecesConsommees) {
        Panne panne = pannes.findById(panneId).orElseThrow(() -> new NotFoundException("Panne introuvable: " + panneId));
        reparation.setPanne(panne);
        reparation.setTechnicien(panne.getTechnicien());
        reparation.setDateDebut(LocalDateTime.now());
        panneService.changerStatut(panneId, StatutPanne.EN_COURS);
        Reparation saved = reparations.save(reparation);
        for (PieceConsommee piece : piecesConsommees) {
            stockService.enregistrerMouvement(
                    piece.pieceId(),
                    piece.quantite(),
                    TypeMouvementStock.CONSOMMATION,
                    "Consommation reparation #" + saved.getId(),
                    saved.getId(),
                    saved.getTechnicien() == null ? null : saved.getTechnicien().getId());
        }
        return saved;
    }

    @Transactional
    public Reparation cloturer(Long id, Integer noteSatisfaction) {
        Reparation reparation = reparations.findById(id)
                .orElseThrow(() -> new NotFoundException("Reparation introuvable: " + id));
        reparation.setDateFin(LocalDateTime.now());
        reparation.setNoteSatisfaction(noteSatisfaction);
        panneService.changerStatut(reparation.getPanne().getId(), StatutPanne.REPAREE);
        return reparations.save(reparation);
    }

    public record PieceConsommee(Long pieceId, int quantite) {
    }
}
