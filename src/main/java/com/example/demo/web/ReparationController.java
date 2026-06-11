package com.example.demo.web;

import com.example.demo.domain.Reparation;
import com.example.demo.repository.ReparationRepository;
import com.example.demo.service.ReparationService;
import com.example.demo.service.ReparationService.PieceConsommee;
import java.util.List;
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

    public ReparationController(ReparationRepository reparations, ReparationService service) {
        this.reparations = reparations;
        this.service = service;
    }

    @GetMapping
    List<Reparation> all() {
        return reparations.findAll();
    }

    @PostMapping
    Reparation create(@RequestBody ReparationRequest request) {
        Reparation reparation = new Reparation();
        reparation.setDescription(request.description());
        reparation.setCoutTotal(request.coutTotal());
        return service.creer(reparation, request.panneId(), request.piecesConsommees() == null ? List.of() : request.piecesConsommees());
    }

    @PutMapping("/{id}/cloturer")
    Reparation cloturer(@PathVariable Long id, @RequestBody ClotureRequest request) {
        return service.cloturer(id, request.noteSatisfaction());
    }

    record ReparationRequest(Long panneId, String description, java.math.BigDecimal coutTotal, List<PieceConsommee> piecesConsommees) {
    }

    record ClotureRequest(Integer noteSatisfaction) {
    }
}
