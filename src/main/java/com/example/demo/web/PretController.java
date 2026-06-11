package com.example.demo.web;

import com.example.demo.domain.Pret;
import com.example.demo.repository.PretRepository;
import com.example.demo.service.PretService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prets")
public class PretController {
    private final PretRepository prets;
    private final PretService service;

    public PretController(PretRepository prets, PretService service) {
        this.prets = prets;
        this.service = service;
    }

    @GetMapping
    List<Pret> all() {
        return prets.findAll();
    }

    @PostMapping
    Pret create(@RequestBody PretRequest request) {
        return service.creer(request.equipementId(), request.employeId(), request.dateRetourPrevue(), request.motif());
    }

    @PutMapping("/{id}/cloturer")
    Pret cloturer(@PathVariable Long id) {
        return service.cloturer(id);
    }

    @PutMapping("/{id}/valider")
    Pret valider(@PathVariable Long id) {
        return service.valider(id);
    }

    @PutMapping("/{id}/refuser")
    Pret refuser(@PathVariable Long id) {
        return service.refuser(id);
    }

    record PretRequest(Long equipementId, Long employeId, LocalDate dateRetourPrevue, String motif) {
    }
}
