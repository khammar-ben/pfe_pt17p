package com.example.demo.web;

import com.example.demo.domain.NiveauUrgence;
import com.example.demo.domain.Panne;
import com.example.demo.domain.StatutPanne;
import com.example.demo.repository.PanneRepository;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.NotFoundException;
import com.example.demo.service.PanneService;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pannes")
public class PanneController {
    private final PanneRepository pannes;
    private final PanneService service;
    private final FileStorageService fileStorageService;

    public PanneController(PanneRepository pannes, PanneService service, FileStorageService fileStorageService) {
        this.pannes = pannes;
        this.service = service;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    List<Panne> all() {
        return pannes.findAll();
    }

    @PostMapping
    Panne declarer(@RequestBody PanneRequest request) {
        Panne panne = new Panne();
        panne.setDescription(request.description());
        panne.setUrgence(request.urgence() == null ? NiveauUrgence.MOYENNE : request.urgence());
        panne.setPhotoPath(request.photoPath());
        return service.declarer(panne, request.equipementId(), request.declarantId());
    }

    @PutMapping("/{id}/affecter/{technicienId}")
    Panne affecter(@PathVariable Long id, @PathVariable Long technicienId) {
        return service.affecter(id, technicienId);
    }

    @PutMapping("/{id}/statut/{statut}")
    Panne changerStatut(@PathVariable Long id, @PathVariable StatutPanne statut) {
        return service.changerStatut(id, statut);
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Panne uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        Panne panne = pannes.findById(id).orElseThrow(() -> new NotFoundException("Panne introuvable: " + id));
        panne.setPhotoPath(fileStorageService.saveImage(file, "pannes"));
        return pannes.save(panne);
    }

    @GetMapping("/{id}/photo")
    ResponseEntity<Resource> photo(@PathVariable Long id) {
        Panne panne = pannes.findById(id).orElseThrow(() -> new NotFoundException("Panne introuvable: " + id));
        if (panne.getPhotoPath() == null || panne.getPhotoPath().isBlank()) {
            throw new NotFoundException("Photo introuvable");
        }
        return ResponseEntity.ok().body(fileStorageService.load(panne.getPhotoPath()));
    }

    record PanneRequest(Long equipementId, Long declarantId, String description, NiveauUrgence urgence, String photoPath) {
    }
}
