package com.example.demo.web;

import com.example.demo.domain.Equipement;
import com.example.demo.domain.Panne;
import com.example.demo.domain.Piece;
import com.example.demo.repository.EquipementRepository;
import com.example.demo.repository.PanneRepository;
import com.example.demo.repository.PieceRepository;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final EquipementRepository equipementRepository;
    private final PieceRepository pieceRepository;
    private final PanneRepository panneRepository;

    public ReportController(
            EquipementRepository equipementRepository,
            PieceRepository pieceRepository,
            PanneRepository panneRepository) {
        this.equipementRepository = equipementRepository;
        this.pieceRepository = pieceRepository;
        this.panneRepository = panneRepository;
    }

    @GetMapping("/equipements.csv")
    public ResponseEntity<byte[]> equipements() {
        StringBuilder csv = new StringBuilder("id,numSerie,type,marque,modele,statut,service,employe\n");
        for (Equipement equipement : equipementRepository.findAll()) {
            csv.append(equipement.getId()).append(',')
                    .append(cell(equipement.getNumSerie())).append(',')
                    .append(cell(equipement.getType())).append(',')
                    .append(cell(equipement.getMarque())).append(',')
                    .append(cell(equipement.getModele())).append(',')
                    .append(cell(equipement.getStatut())).append(',')
                    .append(cell(equipement.getService() == null ? null : equipement.getService().getNom())).append(',')
                    .append(cell(equipement.getEmploye() == null ? null : equipement.getEmploye().getNom()))
                    .append('\n');
        }
        return csv("equipements.csv", csv.toString());
    }

    @GetMapping("/stock.csv")
    public ResponseEntity<byte[]> stock() {
        StringBuilder csv = new StringBuilder("id,reference,designation,quantiteStock,seuilMinimum,localisation,prixUnitaire\n");
        for (Piece piece : pieceRepository.findAll()) {
            csv.append(piece.getId()).append(',')
                    .append(cell(piece.getReference())).append(',')
                    .append(cell(piece.getDesignation())).append(',')
                    .append(piece.getQuantiteStock()).append(',')
                    .append(piece.getSeuilMinimum()).append(',')
                    .append(cell(piece.getLocalisation())).append(',')
                    .append(cell(piece.getPrixUnitaire()))
                    .append('\n');
        }
        return csv("stock.csv", csv.toString());
    }

    @GetMapping("/pannes.csv")
    public ResponseEntity<byte[]> pannes() {
        StringBuilder csv = new StringBuilder("id,description,urgence,statut,dateDeclaration,equipement,technicien\n");
        for (Panne panne : panneRepository.findAll()) {
            csv.append(panne.getId()).append(',')
                    .append(cell(panne.getDescription())).append(',')
                    .append(cell(panne.getUrgence())).append(',')
                    .append(cell(panne.getStatut())).append(',')
                    .append(cell(panne.getDateDeclaration())).append(',')
                    .append(cell(panne.getEquipement() == null ? null : panne.getEquipement().getNumSerie())).append(',')
                    .append(cell(panne.getTechnicien() == null ? null : panne.getTechnicien().getLogin()))
                    .append('\n');
        }
        return csv("pannes.csv", csv.toString());
    }

    private ResponseEntity<byte[]> csv(String filename, String content) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(content.getBytes(StandardCharsets.UTF_8));
    }

    private String cell(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
