package com.example.demo.web;

import com.example.demo.domain.Equipement;
import com.example.demo.domain.HistoriqueEquipement;
import com.example.demo.repository.EquipementRepository;
import com.example.demo.repository.HistoriqueEquipementRepository;
import com.example.demo.service.EquipementService;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.NotFoundException;
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
@RequestMapping("/api/equipements")
public class EquipementController {
    private final EquipementRepository equipements;
    private final HistoriqueEquipementRepository historiques;
    private final EquipementService service;
    private final FileStorageService fileStorageService;

    public EquipementController(EquipementRepository equipements, HistoriqueEquipementRepository historiques,
            EquipementService service, FileStorageService fileStorageService) {
        this.equipements = equipements;
        this.historiques = historiques;
        this.service = service;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    List<Equipement> all() {
        return equipements.findAll();
    }

    @GetMapping("/{id}")
    Equipement one(@PathVariable Long id) {
        return equipements.findById(id).orElseThrow(() -> new NotFoundException("Equipement introuvable: " + id));
    }

    @PostMapping
    Equipement create(@RequestBody Equipement equipement) {
        return service.save(equipement);
    }

    @PutMapping("/{id}")
    Equipement update(@PathVariable Long id, @RequestBody Equipement input) {
        Equipement equipement = one(id);
        equipement.setNumSerie(input.getNumSerie());
        equipement.setType(input.getType());
        equipement.setMarque(input.getMarque());
        equipement.setModele(input.getModele());
        equipement.setStatut(input.getStatut());
        equipement.setEmploye(input.getEmploye());
        equipement.setService(input.getService());
        equipement.setValeur(input.getValeur());
        equipement.setDateAchat(input.getDateAchat());
        equipement.setGarantieFin(input.getGarantieFin());
        return service.save(equipement);
    }

    @GetMapping("/{id}/historique")
    List<HistoriqueEquipement> historique(@PathVariable Long id) {
        return historiques.findByEquipementIdOrderByDateHeureDesc(id);
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Equipement uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        Equipement equipement = one(id);
        equipement.setPhotoPath(fileStorageService.saveImage(file, "equipements"));
        return service.save(equipement);
    }

    @GetMapping("/{id}/photo")
    ResponseEntity<Resource> photo(@PathVariable Long id) {
        Equipement equipement = one(id);
        if (equipement.getPhotoPath() == null || equipement.getPhotoPath().isBlank()) {
            throw new NotFoundException("Photo introuvable");
        }
        return ResponseEntity.ok().body(fileStorageService.load(equipement.getPhotoPath()));
    }
}
