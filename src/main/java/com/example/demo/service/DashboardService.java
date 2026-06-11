package com.example.demo.service;

import com.example.demo.domain.StatutEquipement;
import com.example.demo.domain.StatutPanne;
import com.example.demo.domain.StatutPret;
import com.example.demo.domain.Panne;
import com.example.demo.domain.Piece;
import com.example.demo.domain.Reparation;
import com.example.demo.repository.EquipementRepository;
import com.example.demo.repository.PanneRepository;
import com.example.demo.repository.PieceRepository;
import com.example.demo.repository.PretRepository;
import com.example.demo.repository.ReparationRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final EquipementRepository equipements;
    private final PanneRepository pannes;
    private final PieceRepository pieces;
    private final PretRepository prets;
    private final ReparationRepository reparations;

    public DashboardService(EquipementRepository equipements, PanneRepository pannes,
            PieceRepository pieces, PretRepository prets, ReparationRepository reparations) {
        this.equipements = equipements;
        this.pannes = pannes;
        this.pieces = pieces;
        this.prets = prets;
        this.reparations = reparations;
    }

    public Map<String, Object> kpis() {
        long totalEquipements = equipements.count();
        long indisponibles = equipements.countByStatut(StatutEquipement.EN_PANNE)
                + equipements.countByStatut(StatutEquipement.REFORME);
        double disponibilite = totalEquipements == 0 ? 100.0 : ((double) (totalEquipements - indisponibles) / totalEquipements) * 100.0;
        List<Piece> allPieces = pieces.findAll();
        List<Panne> allPannes = pannes.findAll();
        List<Reparation> allReparations = reparations.findAll();
        long stockCritique = allPieces.stream().filter(piece -> piece.getQuantiteStock() < piece.getSeuilMinimum()).count();
        BigDecimal coutTotal = allReparations.stream()
                .map(Reparation::getCoutTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalEquipements", totalEquipements);
        summary.put("pannesEnCours", pannes.countByStatutNot(StatutPanne.CLOTUREE));
        summary.put("reparations", reparations.count());
        summary.put("pretsEnRetard", prets.countByStatut(StatutPret.EN_RETARD));
        summary.put("stockCritique", stockCritique);
        summary.put("tauxDisponibilite", Math.round(disponibilite * 100.0) / 100.0);
        summary.put("coutTotalReparations", coutTotal);
        summary.put("pannesParStatut", countByStatus(allPannes));
        summary.put("pannesParMois", pannesParMois(allPannes));
        summary.put("reparationsParTechnicien", reparationsParTechnicien(allReparations));
        summary.put("topEquipementsDefaillants", topEquipementsDefaillants(allPannes));
        summary.put("piecesStockCritique", piecesStockCritique(allPieces));
        return summary;
    }

    private Map<String, Long> countByStatus(List<Panne> allPannes) {
        return allPannes.stream()
                .collect(Collectors.groupingBy(panne -> panne.getStatut().name(), LinkedHashMap::new, Collectors.counting()));
    }

    private Map<String, Long> pannesParMois(List<Panne> allPannes) {
        return allPannes.stream()
                .filter(panne -> panne.getDateDeclaration() != null)
                .collect(Collectors.groupingBy(
                        panne -> YearMonth.from(panne.getDateDeclaration()).toString(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }

    private Map<String, Long> reparationsParTechnicien(List<Reparation> allReparations) {
        return allReparations.stream()
                .filter(reparation -> reparation.getTechnicien() != null)
                .collect(Collectors.groupingBy(
                        reparation -> reparation.getTechnicien().getLogin(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }

    private List<Map<String, Object>> topEquipementsDefaillants(List<Panne> allPannes) {
        return allPannes.stream()
                .filter(panne -> panne.getEquipement() != null)
                .collect(Collectors.groupingBy(panne -> panne.getEquipement().getNumSerie(), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(entry -> Map.<String, Object>of("label", entry.getKey(), "value", entry.getValue()))
                .toList();
    }

    private List<Map<String, Object>> piecesStockCritique(List<Piece> allPieces) {
        return allPieces.stream()
                .filter(Piece::stockCritique)
                .sorted(Comparator.comparingInt(Piece::getQuantiteStock))
                .map(piece -> Map.<String, Object>of(
                        "reference", piece.getReference(),
                        "designation", piece.getDesignation(),
                        "quantiteStock", piece.getQuantiteStock(),
                        "seuilMinimum", piece.getSeuilMinimum()
                ))
                .toList();
    }
}
