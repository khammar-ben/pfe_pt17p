package com.example.demo.web;

import com.example.demo.domain.Reparation;
import com.example.demo.domain.StatutPanne;
import com.example.demo.repository.ReparationRepository;
import com.example.demo.service.NotFoundException;
import com.example.demo.service.PdfReportService;
import com.example.demo.service.ReparationService;
import com.example.demo.service.ReparationService.PieceConsommee;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reparations")
public class ReparationController {
    private final ReparationRepository reparations;
    private final ReparationService service;
    private final PdfReportService pdfReportService;

    public ReparationController(ReparationRepository reparations, ReparationService service,
            PdfReportService pdfReportService) {
        this.reparations = reparations;
        this.service = service;
        this.pdfReportService = pdfReportService;
    }

    @GetMapping
    List<Reparation> all(Authentication authentication) {
        if (hasRole(authentication, "EMPLOYE")) {
            return reparations.findByPanneDeclarantLogin(authentication.getName());
        }
        if (hasRole(authentication, "TECHNICIEN")) {
            return reparations.findByTechnicienLogin(authentication.getName());
        }
        return reparations.findAll();
    }

    @PostMapping
    Reparation create(@RequestBody ReparationRequest request) {
        Reparation reparation = new Reparation();
        reparation.setDescription(request.description());
        reparation.setCoutTotal(request.coutTotal());
        return service.creer(reparation, request.panneId(), request.technicienId(),
                request.piecesConsommees() == null ? List.of() : request.piecesConsommees());
    }

    @PutMapping("/{id}/cloturer")
    Reparation cloturer(@PathVariable Long id, @RequestBody ClotureRequest request,
            Authentication authentication) {
        return service.cloturer(
                id,
                request.noteSatisfaction(),
                authentication.getName(),
                hasRole(authentication, "ADMIN"));
    }

    @PutMapping("/{id}/executer")
    Reparation executer(@PathVariable Long id, @RequestBody ExecutionRequest request,
            Authentication authentication) {
        return service.executer(
                id,
                request.diagnostic(),
                request.coutTotal(),
                request.piecesConsommees() == null ? List.of() : request.piecesConsommees(),
                request.resultat(),
                authentication.getName());
    }

    @GetMapping("/{id}/rapport.pdf")
    ResponseEntity<byte[]> rapport(@PathVariable Long id) {
        Reparation reparation = reparations.findById(id)
                .orElseThrow(() -> new NotFoundException("Reparation introuvable: " + id));
        List<String> lines = List.of(
                "Reparation #" + reparation.getId(),
                "Panne: " + (reparation.getPanne() == null ? "" : "#" + reparation.getPanne().getId()),
                "Technicien: " + (reparation.getTechnicien() == null ? "" : reparation.getTechnicien().getLogin()),
                "Consigne: " + reparation.getDescription(),
                "Diagnostic: " + reparation.getDiagnostic(),
                "Debut: " + reparation.getDateDebut(),
                "Fin: " + reparation.getDateFin(),
                "Cout: " + reparation.getCoutTotal(),
                "Satisfaction: " + reparation.getNoteSatisfaction()
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("reparation-" + id + ".pdf").build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfReportService.create("Bon d'intervention", lines));
    }

    record ReparationRequest(Long panneId, Long technicienId, String description,
            java.math.BigDecimal coutTotal, List<PieceConsommee> piecesConsommees) {
    }

    record ClotureRequest(Integer noteSatisfaction) {
    }

    record ExecutionRequest(String diagnostic, java.math.BigDecimal coutTotal,
            List<PieceConsommee> piecesConsommees, StatutPanne resultat) {
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}
