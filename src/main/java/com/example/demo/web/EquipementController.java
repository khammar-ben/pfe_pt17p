package com.example.demo.web;

import com.example.demo.domain.Equipement;
import com.example.demo.domain.Employe;
import com.example.demo.domain.DemandeEquipement;
import com.example.demo.domain.HistoriqueEquipement;
import com.example.demo.domain.Piece;
import com.example.demo.domain.StatutDemandeEquipement;
import com.example.demo.domain.StatutEquipement;
import com.example.demo.domain.UsagePiece;
import com.example.demo.repository.EmployeRepository;
import com.example.demo.repository.DemandeEquipementRepository;
import com.example.demo.repository.EquipementRepository;
import com.example.demo.repository.HistoriqueEquipementRepository;
import com.example.demo.repository.PieceRepository;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.service.EquipementService;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.NotFoundException;
import com.example.demo.service.NotificationInterneService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/equipements")
public class EquipementController {
    private final EquipementRepository equipements;
    private final EmployeRepository employes;
    private final PieceRepository pieces;
    private final DemandeEquipementRepository demandes;
    private final UtilisateurRepository utilisateurs;
    private final HistoriqueEquipementRepository historiques;
    private final EquipementService service;
    private final FileStorageService fileStorageService;
    private final NotificationInterneService notificationInterneService;

    public EquipementController(EquipementRepository equipements, EmployeRepository employes, PieceRepository pieces,
            DemandeEquipementRepository demandes, UtilisateurRepository utilisateurs,
            HistoriqueEquipementRepository historiques, EquipementService service,
            FileStorageService fileStorageService, NotificationInterneService notificationInterneService) {
        this.equipements = equipements;
        this.employes = employes;
        this.pieces = pieces;
        this.demandes = demandes;
        this.utilisateurs = utilisateurs;
        this.historiques = historiques;
        this.service = service;
        this.fileStorageService = fileStorageService;
        this.notificationInterneService = notificationInterneService;
    }

    @GetMapping
    List<Equipement> all(Authentication authentication) {
        if (hasRole(authentication, "EMPLOYE")) {
            return equipements.findByUtilisateurLogin(authentication.getName());
        }
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

    @PutMapping("/affecter")
    Equipement affecter(@RequestBody AffectationRequest request) {
        Equipement equipement = one(request.equipementId());
        Employe employe = employes.findById(request.employeId())
                .orElseThrow(() -> new NotFoundException("Employe introuvable: " + request.employeId()));
        equipement.setEmploye(employe);
        equipement.setService(employe.getService());
        equipement.setStatut(StatutEquipement.AFFECTE);
        service.ajouterHistorique(equipement, "EQUIPEMENT_AFFECTE", employe.getNom() + " " + employe.getPrenom());
        return equipements.save(equipement);
    }

    @PutMapping("/affecter-piece")
    Equipement affecterPiece(@RequestBody PieceAffectationRequest request) {
        DemandeEquipement demande = demandes.findById(request.demandeId())
                .orElseThrow(() -> new NotFoundException("Demande introuvable: " + request.demandeId()));
        if (demande.getStatut() != StatutDemandeEquipement.EN_ATTENTE) {
            throw new IllegalArgumentException("Demande deja traitee");
        }
        Piece piece = demande.getPiece();
        Employe employe = demande.getEmploye();
        return affecterPieceDemandee(piece, employe, demande, request.technicienLogin(), false);
    }

    @PutMapping("/affecter-piece-directe")
    Equipement affecterPieceDirectement(@RequestBody PieceAffectationRequest request, Authentication authentication) {
        Piece piece = pieces.findById(request.pieceId())
                .orElseThrow(() -> new NotFoundException("Piece introuvable: " + request.pieceId()));
        Employe employe = employes.findById(request.employeId())
                .orElseThrow(() -> new NotFoundException("Employe introuvable: " + request.employeId()));
        if (piece.getQuantiteStock() <= 0) {
            throw new IllegalArgumentException("Stock indisponible pour cette piece");
        }
        Equipement saved = affecterPieceDemandee(piece, employe, null, authentication.getName(), true);
        notificationInterneService.notifierTechniciens(
                "Nouvelle affectation d'equipement",
                piece.getReference() + " affecte a " + employe.getNom() + " " + employe.getPrenom(),
                "EQUIPEMENT_AFFECTE",
                saved.getId());
        return saved;
    }

    @PutMapping("/affecter-pack")
    @Transactional
    List<Equipement> affecterPack(@RequestBody PackAffectationRequest request) {
        if (request.pieceIds() == null || request.pieceIds().isEmpty()) {
            throw new IllegalArgumentException("Selectionnez au moins un materiel pour le pack");
        }
        Employe employe = employes.findById(request.employeId())
                .orElseThrow(() -> new NotFoundException("Employe introuvable: " + request.employeId()));
        List<Long> idsUniques = request.pieceIds().stream().distinct().toList();
        List<Piece> materiels = pieces.findAllById(idsUniques);
        if (materiels.size() != idsUniques.size()) {
            throw new IllegalArgumentException("Un materiel du pack est introuvable");
        }
        Set<String> types = materiels.stream()
                .map(this::categorieMateriel)
                .collect(Collectors.toSet());
        if (types.size() != materiels.size()) {
            throw new IllegalArgumentException("Un pack ne peut pas contenir deux materiels de la meme categorie");
        }
        for (Piece piece : materiels) {
            if (piece.getUsage() != UsagePiece.MATERIEL) {
                throw new IllegalArgumentException(piece.getReference() + " est une piece de rechange, pas un materiel de pack");
            }
            if (piece.getQuantiteStock() <= 0) {
                throw new IllegalArgumentException("Stock indisponible pour " + piece.getReference());
            }
        }

        String packReference = "PACK-" + employe.getId() + "-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        List<Equipement> affectes = new ArrayList<>();
        for (Piece piece : materiels) {
            piece.setQuantiteStock(piece.getQuantiteStock() - 1);
            pieces.save(piece);

            Equipement equipement = new Equipement();
            equipement.setNumSerie(nextNumSerie(piece.getReference()));
            equipement.setType(piece.getDesignation());
            equipement.setMarque(piece.getFournisseur() == null ? null : piece.getFournisseur().getNom());
            equipement.setValeur(piece.getPrixUnitaire());
            equipement.setEmploye(employe);
            equipement.setService(employe.getService());
            equipement.setPackReference(packReference);
            equipement.setStatut(StatutEquipement.AFFECTE);
            Equipement saved = equipements.save(equipement);
            service.ajouterHistorique(saved, "PACK_INTEGRATION_AFFECTE",
                    packReference + " - " + employe.getNom() + " " + employe.getPrenom());
            affectes.add(saved);
        }
        notificationInterneService.notifierAdmins(
                "Pack d'integration affecte",
                packReference + " affecte a " + employe.getNom() + " " + employe.getPrenom()
                        + " (" + affectes.size() + " materiels)",
                "PACK_AFFECTE",
                employe.getId());
        return affectes;
    }

    @GetMapping("/demandes")
    List<DemandeEquipement> demandes(Authentication authentication) {
        if (hasRole(authentication, "EMPLOYE")) {
            Long employeId = utilisateurs.findByLogin(authentication.getName())
                    .filter(utilisateur -> utilisateur.getEmploye() != null)
                    .map(utilisateur -> utilisateur.getEmploye().getId())
                    .orElse(null);
            return employeId == null ? List.of() : demandes.findByEmployeIdOrderByDateDemandeDesc(employeId);
        }
        return demandes.findAllByOrderByDateDemandeDesc();
    }

    @PostMapping("/demandes")
    DemandeEquipement demander(@RequestBody PieceAffectationRequest request, Authentication authentication) {
        Piece piece = pieces.findById(request.pieceId())
                .orElseThrow(() -> new NotFoundException("Piece introuvable: " + request.pieceId()));
        Employe employe = employes.findById(request.employeId())
                .orElseThrow(() -> new NotFoundException("Employe introuvable: " + request.employeId()));
        String pieceLabel = (piece.getReference() + " " + piece.getDesignation()).toUpperCase();
        if (!pieceLabel.contains("PC")) {
            throw new IllegalArgumentException("Selectionnez une piece PC");
        }
        if (piece.getQuantiteStock() <= 0) {
            throw new IllegalArgumentException("Stock indisponible pour cette piece");
        }

        DemandeEquipement demande = new DemandeEquipement();
        demande.setPiece(piece);
        demande.setEmploye(employe);
        demande.setDemandeur(utilisateurs.findByLogin(authentication.getName()).orElse(null));
        demande.setStatut(StatutDemandeEquipement.EN_ATTENTE);
        DemandeEquipement saved = demandes.save(demande);
        notificationInterneService.notifierAdmins(
                "Nouvelle demande d'equipement",
                employe.getNom() + " " + employe.getPrenom() + " demande " + piece.getReference(),
                "DEMANDE_EQUIPEMENT",
                saved.getId());
        return saved;
    }

    @PutMapping("/demandes/{id}/refuser")
    DemandeEquipement refuserDemande(@PathVariable Long id, Authentication authentication) {
        requireTechnicien(authentication);
        DemandeEquipement demande = demandes.findById(id)
                .orElseThrow(() -> new NotFoundException("Demande introuvable: " + id));
        demande.setStatut(StatutDemandeEquipement.REFUSEE);
        demande.setDateTraitement(LocalDateTime.now());
        demande.setTechnicien(utilisateurs.findByLogin(authentication.getName()).orElse(null));
        return demandes.save(demande);
    }

    private Equipement affecterPieceDemandee(Piece piece, Employe employe, DemandeEquipement demande,
            String technicienLogin, boolean enAttenteTechnicien) {
        piece.setQuantiteStock(piece.getQuantiteStock() - 1);
        pieces.save(piece);

        Equipement equipement = new Equipement();
        equipement.setNumSerie(nextNumSerie(piece.getReference()));
        equipement.setType(piece.getDesignation());
        equipement.setMarque(piece.getFournisseur() == null ? null : piece.getFournisseur().getNom());
        equipement.setValeur(piece.getPrixUnitaire());
        equipement.setEmploye(employe);
        equipement.setService(employe.getService());
        equipement.setStatut(enAttenteTechnicien
                ? StatutEquipement.EN_ATTENTE_AFFECTATION
                : StatutEquipement.AFFECTE);
        Equipement saved = equipements.save(equipement);
        service.ajouterHistorique(
                saved,
                enAttenteTechnicien ? "EQUIPEMENT_EN_ATTENTE_AFFECTATION" : "EQUIPEMENT_AFFECTE",
                employe.getNom() + " " + employe.getPrenom());
        if (demande != null) {
            demande.setEquipement(saved);
            demande.setStatut(StatutDemandeEquipement.AFFECTEE);
            demande.setDateTraitement(LocalDateTime.now());
            demande.setTechnicien(utilisateurs.findByLogin(technicienLogin).orElse(null));
            demandes.save(demande);
        }
        return saved;
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

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    private void requireTechnicien(Authentication authentication) {
        if (!hasRole(authentication, "TECHNICIEN") && !hasRole(authentication, "ADMIN")) {
            throw new IllegalArgumentException("Action reservee au technicien");
        }
    }

    private String nextNumSerie(String reference) {
        String base = reference == null || reference.isBlank() ? "PC" : reference.replaceAll("[^A-Za-z0-9-]", "-");
        String numSerie = base + "-" + System.currentTimeMillis();
        while (equipements.findByNumSerie(numSerie).isPresent()) {
            numSerie = base + "-" + System.nanoTime();
        }
        return numSerie;
    }

    private String categorieMateriel(Piece piece) {
        String label = (piece.getReference() + " " + piece.getDesignation()).toUpperCase();
        if (label.contains("PC")) return "ORDINATEUR";
        if (label.contains("ECRAN")) return "ECRAN";
        if (label.contains("CLAVIER")) return "CLAVIER";
        if (label.contains("SOURIS")) return "SOURIS";
        return piece.getReference();
    }

    record AffectationRequest(Long equipementId, Long employeId) {
    }

    record PieceAffectationRequest(Long pieceId, Long employeId, Long demandeId, String technicienLogin) {
    }

    record PackAffectationRequest(Long employeId, List<Long> pieceIds) {
    }
}
