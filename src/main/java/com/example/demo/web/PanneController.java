package com.example.demo.web;

import com.example.demo.domain.NiveauUrgence;
import com.example.demo.domain.Panne;
import com.example.demo.domain.Piece;
import com.example.demo.domain.StatutPanne;
import com.example.demo.repository.PanneRepository;
import com.example.demo.repository.PieceRepository;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.NotFoundException;
import com.example.demo.service.PanneService;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.Authentication;
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
    private final UtilisateurRepository utilisateurs;
    private final PieceRepository pieces;
    private final PanneService service;
    private final FileStorageService fileStorageService;

    public PanneController(PanneRepository pannes, UtilisateurRepository utilisateurs, PieceRepository pieces,
            PanneService service, FileStorageService fileStorageService) {
        this.pannes = pannes;
        this.utilisateurs = utilisateurs;
        this.pieces = pieces;
        this.service = service;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    List<Panne> all(Authentication authentication) {
        if (hasRole(authentication, "EMPLOYE")) {
            return pannes.findByDeclarantLogin(authentication.getName());
        }
        if (hasRole(authentication, "TECHNICIEN")) {
            return pannes.findForTechnicien(authentication.getName(), StatutPanne.A_AFFECTER);
        }
        return pannes.findAll();
    }

    @PostMapping
    Panne declarer(@RequestBody PanneRequest request, Authentication authentication) {
        Panne panne = new Panne();
        panne.setDescription(request.description());
        panne.setUrgence(request.urgence() == null ? NiveauUrgence.MOYENNE : request.urgence());
        panne.setPhotoPath(request.photoPath());
        Long declarantId = request.declarantId();
        if (declarantId == null && authentication != null) {
            declarantId = utilisateurs.findByLogin(authentication.getName())
                    .map(utilisateur -> utilisateur.getId())
                    .orElse(null);
        }
        if (request.pieceId() != null) {
            return service.declarerDepuisStock(panne, request.pieceId(), declarantId);
        }
        return service.declarer(panne, request.equipementId(), declarantId);
    }

    @PostMapping("/publier")
    Panne declarerEtPublier(@RequestBody PanneRequest request, Authentication authentication) {
        Panne panne = new Panne();
        panne.setDescription(request.description());
        panne.setUrgence(request.urgence() == null ? NiveauUrgence.MOYENNE : request.urgence());
        panne.setPhotoPath(request.photoPath());
        Long declarantId = request.declarantId();
        if (declarantId == null && authentication != null) {
            declarantId = utilisateurs.findByLogin(authentication.getName())
                    .map(utilisateur -> utilisateur.getId())
                    .orElse(null);
        }
        if (request.pieceId() != null) {
            // Pour l'instant on ne gère pas ce cas, on pourrait ajouter un service declarerEtPublierDepuisStock
            throw new UnsupportedOperationException("La déclaration de panne sur une pièce du stock avec publication immédiate n'est pas supportée.");
        }
        return service.declarerEtPublier(panne, request.equipementId(), declarantId);
    }

    @GetMapping("/types-equipement")
    List<String> typesEquipement() {
        return pieces.findAll().stream()
                .filter(piece -> piece.getQuantiteStock() > 0)
                .map(Piece::getReference)
                .filter(reference -> reference != null && reference.contains("-"))
                .map(reference -> reference.substring(0, reference.indexOf('-')).toUpperCase(Locale.ROOT))
                .distinct()
                .sorted()
                .toList();
    }

    @GetMapping("/pieces-stock")
    List<Piece> piecesStock(@RequestParam String prefix) {
        String normalizedPrefix = prefix == null ? "" : prefix.trim();
        if (normalizedPrefix.isBlank() || !normalizedPrefix.matches("[A-Za-z0-9]+")) {
            throw new IllegalArgumentException("Prefixe de reference invalide");
        }
        return pieces.findAvailableByReferencePrefix(normalizedPrefix);
    }

    @PutMapping("/{id}/affecter/{technicienId}")
    Panne affecter(@PathVariable Long id, @PathVariable Long technicienId) {
        return service.affecter(id, technicienId);
    }

    @PutMapping("/{id}/publier")
    Panne publier(@PathVariable Long id) {
        return service.publier(id);
    }

    @PutMapping("/{id}/claim")
    Panne claim(@PathVariable Long id, Authentication authentication) {
        return service.claim(id, authentication.getName());
    }

    @PutMapping("/{id}/statut/{statut}")
    Panne changerStatut(@PathVariable Long id, @PathVariable StatutPanne statut,
            Authentication authentication) {
        return service.changerStatut(id, statut, authentication.getName(), hasRole(authentication, "ADMIN"));
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

    record PanneRequest(Long equipementId, Long pieceId, Long declarantId, String description,
            NiveauUrgence urgence, String photoPath) {
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}
