package com.example.demo.config;

import com.example.demo.domain.Employe;
import com.example.demo.domain.Equipement;
import com.example.demo.domain.Fournisseur;
import com.example.demo.domain.MouvementStock;
import com.example.demo.domain.NiveauUrgence;
import com.example.demo.domain.Panne;
import com.example.demo.domain.Piece;
import com.example.demo.domain.Pret;
import com.example.demo.domain.Reparation;
import com.example.demo.domain.RoleType;
import com.example.demo.domain.ServiceDepartement;
import com.example.demo.domain.StatutEquipement;
import com.example.demo.domain.StatutPanne;
import com.example.demo.domain.StatutPret;
import com.example.demo.domain.TypeMouvementStock;
import com.example.demo.domain.Utilisateur;
import com.example.demo.repository.EmployeRepository;
import com.example.demo.repository.EquipementRepository;
import com.example.demo.repository.FournisseurRepository;
import com.example.demo.repository.MouvementStockRepository;
import com.example.demo.repository.PanneRepository;
import com.example.demo.repository.PieceRepository;
import com.example.demo.repository.PretRepository;
import com.example.demo.repository.ReparationRepository;
import com.example.demo.repository.ServiceDepartementRepository;
import com.example.demo.repository.UtilisateurRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedData(ServiceDepartementRepository services, EmployeRepository employes,
            UtilisateurRepository utilisateurs, EquipementRepository equipements,
            FournisseurRepository fournisseurs, PieceRepository pieces, PanneRepository pannes,
            ReparationRepository reparations, PretRepository prets, MouvementStockRepository mouvements,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (services.count() == 0) {
                ServiceDepartement informatique = service("Informatique", "Responsable SI", "Gestion du parc informatique");
                ServiceDepartement finance = service("Finance", "Chef comptable", "Service finance et comptabilite");
                ServiceDepartement direction = service("Direction", "Directeur General", "Direction generale");
                services.saveAll(List.of(informatique, finance, direction));
            }
            List<ServiceDepartement> serviceList = services.findAll();
            ServiceDepartement informatique = serviceList.get(0);
            ServiceDepartement finance = serviceList.size() > 1 ? serviceList.get(1) : informatique;
            ServiceDepartement direction = serviceList.size() > 2 ? serviceList.get(2) : informatique;

            if (employes.count() == 0) {
                employes.saveAll(List.of(
                        employe("Lahlou", "Ahmed", "ahmed@example.com", "Comptable", "+212600000001", finance),
                        employe("Bennani", "Sara", "sara@example.com", "Assistante direction", "+212600000002", direction),
                        employe("El Amrani", "Youssef", "youssef@example.com", "Developpeur", "+212600000003", informatique),
                        employe("Ziani", "Nadia", "nadia@example.com", "RH", "+212600000004", direction)
                ));
            }
            List<Employe> employeList = employes.findAll();
            Employe ahmed = employeList.get(0);
            Employe sara = employeList.size() > 1 ? employeList.get(1) : ahmed;
            Employe youssef = employeList.size() > 2 ? employeList.get(2) : ahmed;

            Utilisateur admin = utilisateurs.findByLogin("admin")
                    .orElseGet(() -> utilisateurs.save(user("admin", passwordEncoder.encode("admin"), "admin@example.com", RoleType.ADMIN, null)));
            Utilisateur tech = utilisateurs.findByLogin("tech")
                    .orElseGet(() -> utilisateurs.save(user("tech", passwordEncoder.encode("tech"), "tech@example.com", RoleType.TECHNICIEN, null)));
            Utilisateur employe = utilisateurs.findByLogin("employe")
                    .orElseGet(() -> utilisateurs.save(user("employe", passwordEncoder.encode("employe"), "employe@example.com", RoleType.EMPLOYE, ahmed)));
            utilisateurs.findByLogin("directeur")
                    .orElseGet(() -> utilisateurs.save(user("directeur", passwordEncoder.encode("directeur"), "directeur@example.com", RoleType.DIRECTEUR, sara)));

            if (fournisseurs.count() == 0) {
                fournisseurs.saveAll(List.of(
                        fournisseur("TechParts", "+212600000100", "Pieces PC", "contact@techparts.test"),
                        fournisseur("CasaNetworks", "+212600000200", "Reseau et peripheriques", "sales@casanetworks.test")
                ));
            }
            List<Fournisseur> fournisseurList = fournisseurs.findAll();
            Fournisseur techParts = fournisseurList.get(0);
            Fournisseur casaNetworks = fournisseurList.size() > 1 ? fournisseurList.get(1) : techParts;

            if (pieces.count() == 0) {
                pieces.saveAll(List.of(
                        piece("RAM-8G-DDR4", "Barrette RAM 8GB DDR4", 3, 5, "Armoire A1", "220.00", techParts),
                        piece("SSD-512-NVME", "Disque SSD NVMe 512GB", 8, 4, "Armoire A2", "480.00", techParts),
                        piece("BAT-DELL-54", "Batterie Dell Latitude", 2, 3, "Armoire B1", "650.00", techParts),
                        piece("RJ45-CAT6", "Cable reseau RJ45 Cat6", 20, 10, "Casier R1", "25.00", casaNetworks),
                        piece("SOURIS-USB", "Souris USB", 5, 8, "Casier P1", "65.00", casaNetworks)
                ));
            }
            List<Piece> pieceList = pieces.findAll();

            if (equipements.count() == 0) {
                equipements.saveAll(List.of(
                        equipement("PC001", "PC portable", "Dell", "Latitude 5420", "8500.00", StatutEquipement.EN_PANNE, ahmed, finance, -1, 8),
                        equipement("PC002", "PC portable", "HP", "EliteBook 840", "9200.00", StatutEquipement.DISPONIBLE, null, informatique, -2, 2),
                        equipement("PC003", "PC fixe", "Lenovo", "ThinkCentre", "6200.00", StatutEquipement.AFFECTE, youssef, informatique, -3, -1),
                        equipement("IMP001", "Imprimante", "Canon", "i-SENSYS", "3400.00", StatutEquipement.DISPONIBLE, null, direction, -2, 5),
                        equipement("SW001", "Switch", "Cisco", "CBS250", "4100.00", StatutEquipement.AFFECTE, null, informatique, -4, 10),
                        equipement("PC004", "PC portable", "Dell", "Vostro", "7300.00", StatutEquipement.EN_PRET, sara, direction, -1, 11),
                        equipement("ECR001", "Ecran", "Samsung", "24 pouces", "1600.00", StatutEquipement.DISPONIBLE, null, finance, -1, 18),
                        equipement("PC005", "PC portable", "Acer", "TravelMate", "5800.00", StatutEquipement.REFORME, null, informatique, -6, -3)
                ));
            }
            List<Equipement> equipementList = equipements.findAll();

            if (pannes.count() == 0) {
                Panne p1 = panne(equipementList.get(0), employe, tech, "Le PC ne demarre plus", NiveauUrgence.HAUTE, StatutPanne.EN_COURS, LocalDateTime.now().minusHours(6));
                Panne p2 = panne(equipementList.get(2), employe, tech, "Lenteur importante au demarrage", NiveauUrgence.MOYENNE, StatutPanne.REPAREE, LocalDateTime.now().minusDays(12));
                Panne p3 = panne(equipementList.get(3), employe, null, "Bourrage papier recurrent", NiveauUrgence.FAIBLE, StatutPanne.DECLAREE, LocalDateTime.now().minusDays(2));
                Panne p4 = panne(equipementList.get(4), admin, tech, "Perte de connectivite intermittente", NiveauUrgence.HAUTE, StatutPanne.EN_ATTENTE_PIECE, LocalDateTime.now().minusDays(5));
                Panne p5 = panne(equipementList.get(6), employe, tech, "Ecran scintille apres 10 minutes", NiveauUrgence.MOYENNE, StatutPanne.CLOTUREE, LocalDateTime.now().minusMonths(1));
                p5.setDateCloture(LocalDateTime.now().minusDays(20));
                pannes.saveAll(List.of(p1, p2, p3, p4, p5));
            }
            List<Panne> panneList = pannes.findAll();

            if (reparations.count() == 0 && !panneList.isEmpty()) {
                reparations.saveAll(List.of(
                        reparation(panneList.get(0), tech, "Diagnostic carte mere et test alimentation", "350.00", null, 4),
                        reparation(panneList.size() > 1 ? panneList.get(1) : panneList.get(0), tech, "Remplacement SSD et nettoyage systeme", "780.00", LocalDateTime.now().minusDays(10), 5),
                        reparation(panneList.size() > 4 ? panneList.get(4) : panneList.get(0), tech, "Remplacement cable video ecran", "180.00", LocalDateTime.now().minusDays(20), 4)
                ));
            }

            if (mouvements.count() == 0 && !pieceList.isEmpty()) {
                mouvements.saveAll(List.of(
                        mouvement(pieceList.get(0), 10, TypeMouvementStock.ENTREE, "Stock initial", null, admin),
                        mouvement(pieceList.size() > 1 ? pieceList.get(1) : pieceList.get(0), 1, TypeMouvementStock.CONSOMMATION, "Reparation PC003", reparations.findAll().stream().findFirst().orElse(null), tech),
                        mouvement(pieceList.size() > 3 ? pieceList.get(3) : pieceList.get(0), 5, TypeMouvementStock.SORTIE, "Remplacement cables salle reunion", null, tech)
                ));
            }

            if (prets.count() == 0 && equipementList.size() > 3) {
                prets.saveAll(List.of(
                        pret(equipementList.get(1), youssef, LocalDate.now().plusDays(7), "Presentation projet", StatutPret.EN_ATTENTE, null),
                        pret(equipementList.get(5), sara, LocalDate.now().plusDays(3), "Mission externe", StatutPret.VALIDE, null),
                        pret(equipementList.get(3), ahmed, LocalDate.now().minusDays(2), "Inventaire agence", StatutPret.EN_RETARD, null),
                        pret(equipementList.get(6), ahmed, LocalDate.now().minusDays(15), "Formation interne", StatutPret.CLOTURE, LocalDate.now().minusDays(12))
                ));
            }
        };
    }

    private ServiceDepartement service(String nom, String responsable, String description) {
        ServiceDepartement service = new ServiceDepartement();
        service.setNom(nom);
        service.setResponsable(responsable);
        service.setDescription(description);
        return service;
    }

    private Employe employe(String nom, String prenom, String email, String poste, String telephone, ServiceDepartement service) {
        Employe employe = new Employe();
        employe.setNom(nom);
        employe.setPrenom(prenom);
        employe.setEmail(email);
        employe.setPoste(poste);
        employe.setTelephone(telephone);
        employe.setService(service);
        return employe;
    }

    private Utilisateur user(String login, String password, String email, RoleType role, Employe employe) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setLogin(login);
        utilisateur.setMotDePasse(password);
        utilisateur.setEmail(email);
        utilisateur.setRole(role);
        utilisateur.setEmploye(employe);
        return utilisateur;
    }

    private Fournisseur fournisseur(String nom, String contact, String specialite, String email) {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setNom(nom);
        fournisseur.setContact(contact);
        fournisseur.setSpecialite(specialite);
        fournisseur.setEmail(email);
        return fournisseur;
    }

    private Piece piece(String reference, String designation, int stock, int seuil, String localisation, String prix, Fournisseur fournisseur) {
        Piece piece = new Piece();
        piece.setReference(reference);
        piece.setDesignation(designation);
        piece.setQuantiteStock(stock);
        piece.setSeuilMinimum(seuil);
        piece.setLocalisation(localisation);
        piece.setPrixUnitaire(new BigDecimal(prix));
        piece.setFournisseur(fournisseur);
        return piece;
    }

    private Equipement equipement(String numSerie, String type, String marque, String modele, String valeur,
            StatutEquipement statut, Employe employe, ServiceDepartement service, int achatYears, int garantieMonths) {
        Equipement equipement = new Equipement();
        equipement.setNumSerie(numSerie);
        equipement.setType(type);
        equipement.setMarque(marque);
        equipement.setModele(modele);
        equipement.setValeur(new BigDecimal(valeur));
        equipement.setStatut(statut);
        equipement.setEmploye(employe);
        equipement.setService(service);
        equipement.setDateAchat(LocalDate.now().plusYears(achatYears));
        equipement.setGarantieFin(LocalDate.now().plusMonths(garantieMonths));
        return equipement;
    }

    private Panne panne(Equipement equipement, Utilisateur declarant, Utilisateur technicien, String description,
            NiveauUrgence urgence, StatutPanne statut, LocalDateTime dateDeclaration) {
        Panne panne = new Panne();
        panne.setEquipement(equipement);
        panne.setDeclarant(declarant);
        panne.setTechnicien(technicien);
        panne.setDescription(description);
        panne.setUrgence(urgence);
        panne.setStatut(statut);
        panne.setDateDeclaration(dateDeclaration);
        return panne;
    }

    private Reparation reparation(Panne panne, Utilisateur technicien, String description, String cout,
            LocalDateTime dateFin, Integer note) {
        Reparation reparation = new Reparation();
        reparation.setPanne(panne);
        reparation.setTechnicien(technicien);
        reparation.setDescription(description);
        reparation.setCoutTotal(new BigDecimal(cout));
        reparation.setDateDebut(panne.getDateDeclaration().plusHours(2));
        reparation.setDateFin(dateFin);
        reparation.setNoteSatisfaction(note);
        return reparation;
    }

    private MouvementStock mouvement(Piece piece, int quantite, TypeMouvementStock type, String motif,
            Reparation reparation, Utilisateur utilisateur) {
        MouvementStock mouvement = new MouvementStock();
        mouvement.setPiece(piece);
        mouvement.setQuantite(quantite);
        mouvement.setTypeMouvement(type);
        mouvement.setMotif(motif);
        mouvement.setReparation(reparation);
        mouvement.setUtilisateur(utilisateur);
        return mouvement;
    }

    private Pret pret(Equipement equipement, Employe employe, LocalDate retourPrevu, String motif,
            StatutPret statut, LocalDate retourReel) {
        Pret pret = new Pret();
        pret.setEquipement(equipement);
        pret.setEmploye(employe);
        pret.setDateDepart(retourPrevu.minusDays(7));
        pret.setDateRetourPrevue(retourPrevu);
        pret.setDateRetourReelle(retourReel);
        pret.setMotif(motif);
        pret.setStatut(statut);
        return pret;
    }
}
