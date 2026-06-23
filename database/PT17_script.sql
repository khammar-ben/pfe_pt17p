CREATE DATABASE IF NOT EXISTS pt17_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE pt17_db;

CREATE TABLE IF NOT EXISTS services (
  id BIGINT NOT NULL AUTO_INCREMENT,
  nom VARCHAR(100) NOT NULL,
  responsable VARCHAR(100),
  description TEXT,
  PRIMARY KEY (id),
  UNIQUE KEY uk_services_nom (nom)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS employes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  nom VARCHAR(100) NOT NULL,
  prenom VARCHAR(100) NOT NULL,
  email VARCHAR(100),
  poste VARCHAR(100),
  telephone VARCHAR(20),
  service_id BIGINT,
  PRIMARY KEY (id),
  UNIQUE KEY uk_employes_email (email),
  CONSTRAINT fk_employes_service FOREIGN KEY (service_id) REFERENCES services(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS utilisateurs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  login VARCHAR(50) NOT NULL,
  mot_de_passe VARCHAR(255) NOT NULL,
  email VARCHAR(100),
  actif BIT NOT NULL DEFAULT 1,
  tentatives_echec INT NOT NULL DEFAULT 0,
  date_creation DATETIME(6),
  role ENUM('ADMIN','TECHNICIEN','EMPLOYE','DIRECTEUR') NOT NULL DEFAULT 'EMPLOYE',
  employe_id BIGINT,
  PRIMARY KEY (id),
  UNIQUE KEY uk_utilisateurs_login (login),
  UNIQUE KEY uk_utilisateurs_email (email),
  CONSTRAINT fk_utilisateurs_employe FOREIGN KEY (employe_id) REFERENCES employes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS fournisseurs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  nom VARCHAR(100) NOT NULL,
  contact VARCHAR(100),
  specialite VARCHAR(100),
  email VARCHAR(100),
  PRIMARY KEY (id),
  UNIQUE KEY uk_fournisseurs_nom (nom)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS equipements (
  id BIGINT NOT NULL AUTO_INCREMENT,
  num_serie VARCHAR(50) NOT NULL,
  type VARCHAR(50) NOT NULL,
  marque VARCHAR(50),
  modele VARCHAR(50),
  date_achat DATE,
  valeur DECIMAL(10,2),
  photo_path VARCHAR(255),
  pack_reference VARCHAR(255),
  garantie_fin DATE,
  statut ENUM('DISPONIBLE','EN_ATTENTE_AFFECTATION','AFFECTE','EN_PRET','EN_PANNE','REFORME') NOT NULL DEFAULT 'DISPONIBLE',
  employe_id BIGINT,
  service_id BIGINT,
  PRIMARY KEY (id),
  UNIQUE KEY uk_equipements_num_serie (num_serie),
  CONSTRAINT fk_equipements_employe FOREIGN KEY (employe_id) REFERENCES employes(id),
  CONSTRAINT fk_equipements_service FOREIGN KEY (service_id) REFERENCES services(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pannes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  description TEXT NOT NULL,
  urgence ENUM('HAUTE','MOYENNE','FAIBLE') NOT NULL DEFAULT 'MOYENNE',
  statut ENUM('DECLAREE','A_AFFECTER','EN_COURS','EN_ATTENTE_PIECE','REPAREE','CLOTUREE') NOT NULL DEFAULT 'DECLAREE',
  date_declaration DATETIME(6),
  date_cloture DATETIME(6),
  photo_path VARCHAR(255),
  equipement_id BIGINT NOT NULL,
  declarant_id BIGINT,
  technicien_id BIGINT,
  PRIMARY KEY (id),
  CONSTRAINT fk_pannes_equipement FOREIGN KEY (equipement_id) REFERENCES equipements(id),
  CONSTRAINT fk_pannes_declarant FOREIGN KEY (declarant_id) REFERENCES utilisateurs(id),
  CONSTRAINT fk_pannes_technicien FOREIGN KEY (technicien_id) REFERENCES utilisateurs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reparations (
  id BIGINT NOT NULL AUTO_INCREMENT,
  description TEXT,
  date_debut DATETIME(6),
  date_fin DATETIME(6),
  cout_total DECIMAL(10,2),
  note_satisfaction INT,
  pdf_path VARCHAR(255),
  panne_id BIGINT,
  technicien_id BIGINT,
  PRIMARY KEY (id),
  CONSTRAINT fk_reparations_panne FOREIGN KEY (panne_id) REFERENCES pannes(id),
  CONSTRAINT fk_reparations_technicien FOREIGN KEY (technicien_id) REFERENCES utilisateurs(id),
  CONSTRAINT chk_reparations_note CHECK (note_satisfaction IS NULL OR note_satisfaction BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pieces (
  id BIGINT NOT NULL AUTO_INCREMENT,
  reference VARCHAR(80) NOT NULL,
  designation VARCHAR(150) NOT NULL,
  quantite_stock INT NOT NULL DEFAULT 0,
  seuil_minimum INT NOT NULL DEFAULT 0,
  localisation VARCHAR(100),
  prix_unitaire DECIMAL(10,2),
  categorie_usage ENUM('MATERIEL','RECHANGE') NOT NULL DEFAULT 'RECHANGE',
  fournisseur_id BIGINT,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pieces_reference (reference),
  CONSTRAINT fk_pieces_fournisseur FOREIGN KEY (fournisseur_id) REFERENCES fournisseurs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS mouvements_stock (
  id BIGINT NOT NULL AUTO_INCREMENT,
  type_mouvement ENUM('ENTREE','SORTIE','CONSOMMATION') NOT NULL,
  quantite INT NOT NULL,
  date_heure DATETIME(6),
  motif VARCHAR(255),
  piece_id BIGINT,
  reparation_id BIGINT,
  utilisateur_id BIGINT,
  PRIMARY KEY (id),
  CONSTRAINT fk_mouvements_piece FOREIGN KEY (piece_id) REFERENCES pieces(id),
  CONSTRAINT fk_mouvements_reparation FOREIGN KEY (reparation_id) REFERENCES reparations(id),
  CONSTRAINT fk_mouvements_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS prets (
  id BIGINT NOT NULL AUTO_INCREMENT,
  date_depart DATE,
  date_retour_prevue DATE,
  date_retour_reelle DATE,
  motif TEXT,
  statut ENUM('EN_ATTENTE','VALIDE','REFUSE','CLOTURE','EN_RETARD') NOT NULL DEFAULT 'EN_ATTENTE',
  equipement_id BIGINT,
  employe_id BIGINT,
  valideur_id BIGINT,
  PRIMARY KEY (id),
  CONSTRAINT fk_prets_equipement FOREIGN KEY (equipement_id) REFERENCES equipements(id),
  CONSTRAINT fk_prets_employe FOREIGN KEY (employe_id) REFERENCES employes(id),
  CONSTRAINT fk_prets_valideur FOREIGN KEY (valideur_id) REFERENCES utilisateurs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE prets
  MODIFY statut ENUM('EN_ATTENTE','VALIDE','REFUSE','CLOTURE','EN_RETARD') NOT NULL DEFAULT 'EN_ATTENTE';

CREATE TABLE IF NOT EXISTS historiques_equipement (
  id BIGINT NOT NULL AUTO_INCREMENT,
  evenement VARCHAR(100),
  date_heure DATETIME(6),
  details TEXT,
  equipement_id BIGINT,
  PRIMARY KEY (id),
  CONSTRAINT fk_historiques_equipement FOREIGN KEY (equipement_id) REFERENCES equipements(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  action VARCHAR(150),
  ip_adresse VARCHAR(80),
  date_heure DATETIME(6),
  details TEXT,
  utilisateur_id BIGINT,
  PRIMARY KEY (id),
  CONSTRAINT fk_audit_logs_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notifications_internes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  titre VARCHAR(255),
  message VARCHAR(255),
  type VARCHAR(100),
  reference_id BIGINT,
  date_creation DATETIME(6),
  lu BIT NOT NULL DEFAULT 0,
  statut ENUM('NOUVELLE','AFFECTEE','EN_COURS','DONE') NOT NULL DEFAULT 'NOUVELLE',
  destinataire_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_notifications_destinataire FOREIGN KEY (destinataire_id) REFERENCES utilisateurs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO services (id, nom, responsable, description)
VALUES
  (1, 'Informatique', 'Responsable SI', 'Gestion du parc informatique'),
  (2, 'Finance', 'Chef comptable', 'Service finance et comptabilite'),
  (3, 'Direction', 'Directeur General', 'Direction generale')
ON DUPLICATE KEY UPDATE responsable = VALUES(responsable), description = VALUES(description);

INSERT INTO employes (id, nom, prenom, email, poste, telephone, service_id)
VALUES
  (1, 'Lahlou', 'Ahmed', 'ahmed@example.com', 'Comptable', '+212600000001', 2),
  (2, 'Bennani', 'Sara', 'sara@example.com', 'Assistante direction', '+212600000002', 3),
  (3, 'El Amrani', 'Youssef', 'youssef@example.com', 'Developpeur', '+212600000003', 1),
  (4, 'Ziani', 'Nadia', 'nadia@example.com', 'RH', '+212600000004', 3)
ON DUPLICATE KEY UPDATE poste = VALUES(poste), telephone = VALUES(telephone), service_id = VALUES(service_id);

INSERT INTO utilisateurs (id, login, mot_de_passe, email, actif, tentatives_echec, date_creation, role, employe_id)
VALUES
  (1, 'admin', 'admin', 'admin@example.com', 1, 0, NOW(6), 'ADMIN', NULL),
  (2, 'tech', 'tech', 'tech@example.com', 1, 0, NOW(6), 'TECHNICIEN', NULL),
  (3, 'employe', 'employe', 'employe@example.com', 1, 0, NOW(6), 'EMPLOYE', 1),
  (4, 'directeur', 'directeur', 'directeur@example.com', 1, 0, NOW(6), 'DIRECTEUR', 2)
ON DUPLICATE KEY UPDATE role = VALUES(role), actif = VALUES(actif), employe_id = VALUES(employe_id);

INSERT INTO fournisseurs (id, nom, contact, specialite, email)
VALUES
  (1, 'TechParts', '+212600000100', 'Pieces PC', 'contact@techparts.test'),
  (2, 'CasaNetworks', '+212600000200', 'Reseau et peripheriques', 'sales@casanetworks.test'),
  (3, 'Atlas Digital', '+212600000300', 'Postes utilisateurs', 'commande@atlasdigital.test'),
  (4, 'PrintOffice', '+212600000400', 'Impression et consommables', 'support@printoffice.test'),
  (5, 'SecureLink', '+212600000500', 'Accessoires et securite', 'contact@securelink.test')
ON DUPLICATE KEY UPDATE contact = VALUES(contact), specialite = VALUES(specialite), email = VALUES(email);

INSERT INTO equipements (id, num_serie, type, marque, modele, date_achat, valeur, garantie_fin, statut, employe_id, service_id)
VALUES
  (1, 'PC001', 'PC portable', 'Dell', 'Latitude 5420', DATE_SUB(CURRENT_DATE, INTERVAL 1 YEAR), 8500.00, DATE_ADD(CURRENT_DATE, INTERVAL 8 MONTH), 'EN_PANNE', 1, 2),
  (2, 'PC002', 'PC portable', 'HP', 'EliteBook 840', DATE_SUB(CURRENT_DATE, INTERVAL 2 YEAR), 9200.00, DATE_ADD(CURRENT_DATE, INTERVAL 2 MONTH), 'DISPONIBLE', NULL, 1),
  (3, 'PC003', 'PC fixe', 'Lenovo', 'ThinkCentre', DATE_SUB(CURRENT_DATE, INTERVAL 3 YEAR), 6200.00, DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH), 'AFFECTE', 3, 1),
  (4, 'IMP001', 'Imprimante', 'Canon', 'i-SENSYS', DATE_SUB(CURRENT_DATE, INTERVAL 2 YEAR), 3400.00, DATE_ADD(CURRENT_DATE, INTERVAL 5 MONTH), 'DISPONIBLE', NULL, 3),
  (5, 'SW001', 'Switch', 'Cisco', 'CBS250', DATE_SUB(CURRENT_DATE, INTERVAL 4 YEAR), 4100.00, DATE_ADD(CURRENT_DATE, INTERVAL 10 MONTH), 'AFFECTE', NULL, 1),
  (6, 'PC004', 'PC portable', 'Dell', 'Vostro', DATE_SUB(CURRENT_DATE, INTERVAL 1 YEAR), 7300.00, DATE_ADD(CURRENT_DATE, INTERVAL 11 MONTH), 'EN_PRET', 2, 3),
  (7, 'ECR001', 'Ecran', 'Samsung', '24 pouces', DATE_SUB(CURRENT_DATE, INTERVAL 1 YEAR), 1600.00, DATE_ADD(CURRENT_DATE, INTERVAL 18 MONTH), 'DISPONIBLE', NULL, 2),
  (8, 'PC005', 'PC portable', 'Acer', 'TravelMate', DATE_SUB(CURRENT_DATE, INTERVAL 6 YEAR), 5800.00, DATE_SUB(CURRENT_DATE, INTERVAL 3 MONTH), 'REFORME', NULL, 1)
ON DUPLICATE KEY UPDATE statut = VALUES(statut), employe_id = VALUES(employe_id), service_id = VALUES(service_id);

INSERT INTO pieces (id, reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
VALUES
  -- Materiel affectable dans les packs collaborateurs
  (1, 'PC-LEN-T14', 'Lenovo ThinkPad T14 i5 16GB 512GB', 12, 3, 'Zone Materiel M1', 13200.00, 'MATERIEL', 3),
  (2, 'PC-HP-840', 'HP EliteBook 840 G8 i5 16GB 512GB', 10, 3, 'Zone Materiel M1', 12800.00, 'MATERIEL', 3),
  (3, 'PC-DELL-7430', 'Dell Latitude 7430 i7 16GB 512GB', 8, 3, 'Zone Materiel M1', 14500.00, 'MATERIEL', 3),
  (4, 'PC-DELL-VOSTRO', 'Dell Vostro 15 i5 8GB 512GB', 14, 4, 'Zone Materiel M2', 7300.00, 'MATERIEL', 3),
  (5, 'PC-MAC-AIR-M2', 'MacBook Air M2 8GB 256GB', 5, 2, 'Zone Materiel M2', 12500.00, 'MATERIEL', 3),
  (6, 'DESK-LEN-M70Q', 'Lenovo ThinkCentre M70q Tiny', 10, 3, 'Zone Materiel M3', 6200.00, 'MATERIEL', 3),
  (7, 'ECRAN-DELL-24', 'Ecran Dell P2422H 24 pouces', 22, 6, 'Rayon Ecrans E1', 2100.00, 'MATERIEL', 3),
  (8, 'ECRAN-HP-27', 'Ecran HP E27 G5 27 pouces', 10, 3, 'Rayon Ecrans E1', 2850.00, 'MATERIEL', 3),
  (9, 'CLAVIER-LOGI-K120', 'Clavier Logitech K120 AZERTY USB', 35, 10, 'Casier Accessoires A1', 120.00, 'MATERIEL', 5),
  (10, 'SOURIS-LOGI-M90', 'Souris Logitech M90 USB', 38, 10, 'Casier Accessoires A1', 85.00, 'MATERIEL', 5),
  (11, 'CASQUE-JABRA-EVOLVE', 'Casque Jabra Evolve 20 USB', 16, 5, 'Casier Accessoires A2', 390.00, 'MATERIEL', 5),
  (12, 'DOCK-DELL-WD19', 'Station accueil Dell WD19 USB-C', 12, 4, 'Casier Accessoires A3', 1550.00, 'MATERIEL', 5),
  (13, 'IMP-HP-LJ-PRO', 'Imprimante HP LaserJet Pro', 6, 2, 'Zone Impression I1', 2800.00, 'MATERIEL', 4),
  (14, 'IMP-CANON-MF', 'Imprimante Canon i-SENSYS MF', 5, 2, 'Zone Impression I1', 3400.00, 'MATERIEL', 4),
  (15, 'SW-CISCO-CBS250', 'Switch Cisco CBS250 24 ports', 8, 2, 'Rack Reseau R1', 4100.00, 'MATERIEL', 2),
  (16, 'AP-UBI-U6-LITE', 'Point acces Ubiquiti UniFi U6 Lite', 14, 4, 'Rack Reseau R2', 980.00, 'MATERIEL', 2),

  -- Pieces de rechange et consommables pour reparations
  (17, 'RAM-8G-DDR4', 'Barrette RAM 8GB DDR4 SODIMM', 10, 4, 'Armoire Rechange B1', 220.00, 'RECHANGE', 1),
  (18, 'RAM-16G-DDR4', 'Barrette RAM 16GB DDR4 SODIMM', 7, 3, 'Armoire Rechange B1', 420.00, 'RECHANGE', 1),
  (19, 'SSD-256-SATA', 'Disque SSD SATA 256GB', 8, 3, 'Armoire Rechange B2', 280.00, 'RECHANGE', 1),
  (20, 'SSD-512-NVME', 'Disque SSD NVMe 512GB', 9, 4, 'Armoire Rechange B2', 480.00, 'RECHANGE', 1),
  (21, 'SSD-1T-NVME', 'Disque SSD NVMe 1TB', 4, 2, 'Armoire Rechange B2', 820.00, 'RECHANGE', 1),
  (22, 'BAT-DELL-54', 'Batterie Dell Latitude 54Wh', 5, 2, 'Armoire Batteries C1', 650.00, 'RECHANGE', 1),
  (23, 'BAT-HP-840', 'Batterie HP EliteBook 840', 4, 2, 'Armoire Batteries C1', 780.00, 'RECHANGE', 1),
  (24, 'CHG-DELL-65W', 'Chargeur Dell USB-C 65W', 8, 3, 'Casier Chargeurs C2', 320.00, 'RECHANGE', 5),
  (25, 'CHG-HP-65W', 'Chargeur HP USB-C 65W', 7, 3, 'Casier Chargeurs C2', 310.00, 'RECHANGE', 5),
  (26, 'ECRAN-DALLE-14', 'Dalle laptop 14 pouces Full HD', 2, 1, 'Armoire Rechange B3', 980.00, 'RECHANGE', 1),
  (27, 'CLAVIER-LEN-T14', 'Clavier Lenovo ThinkPad T14 AZERTY', 3, 1, 'Armoire Rechange B3', 540.00, 'RECHANGE', 1),
  (28, 'VENTILO-LEN-T14', 'Ventilateur Lenovo ThinkPad T14', 3, 1, 'Armoire Rechange B4', 260.00, 'RECHANGE', 1),
  (29, 'PATE-THERMIQUE', 'Pate thermique processeur', 12, 4, 'Casier Atelier W1', 55.00, 'RECHANGE', 1),
  (30, 'TONER-HP-135A', 'Toner HP 135A noir', 6, 2, 'Zone Impression I2', 690.00, 'RECHANGE', 4),
  (31, 'TONER-CANON-057', 'Toner Canon 057 noir', 4, 2, 'Zone Impression I2', 760.00, 'RECHANGE', 4),
  (32, 'RJ45-CAT6-2M', 'Cable reseau RJ45 Cat6 2m', 35, 12, 'Casier Reseau N1', 25.00, 'RECHANGE', 2),
  (33, 'RJ45-CAT6-5M', 'Cable reseau RJ45 Cat6 5m', 20, 8, 'Casier Reseau N1', 45.00, 'RECHANGE', 2),
  (34, 'HDMI-2M', 'Cable HDMI 2m', 18, 6, 'Casier Accessoires A4', 45.00, 'RECHANGE', 5),
  (35, 'ADAPT-USBC-HDMI', 'Adaptateur USB-C vers HDMI', 7, 3, 'Casier Accessoires A4', 180.00, 'RECHANGE', 5),
  (36, 'ADAPT-USBC-RJ45', 'Adaptateur USB-C vers RJ45', 6, 3, 'Casier Accessoires A4', 220.00, 'RECHANGE', 5),
  (37, 'ALIM-SW-CISCO', 'Alimentation switch Cisco CBS250', 2, 1, 'Rack Reseau R3', 640.00, 'RECHANGE', 2),
  (38, 'PATCH-CAT6-0M5', 'Patch cable RJ45 Cat6 0.5m', 40, 15, 'Casier Reseau N2', 18.00, 'RECHANGE', 2)
ON DUPLICATE KEY UPDATE
  designation = VALUES(designation),
  quantite_stock = VALUES(quantite_stock),
  seuil_minimum = VALUES(seuil_minimum),
  localisation = VALUES(localisation),
  prix_unitaire = VALUES(prix_unitaire),
  categorie_usage = VALUES(categorie_usage),
  fournisseur_id = VALUES(fournisseur_id);

UPDATE pieces
SET categorie_usage = CASE
  WHEN UPPER(reference) REGEXP '^(PC-|DESK-|ECRAN-|IMP-|SW-|AP-|CLAVIER-LOGI|SOURIS-LOGI|CASQUE-|DOCK-)'
    THEN 'MATERIEL'
  ELSE 'RECHANGE'
END
WHERE categorie_usage IS NULL;

INSERT INTO pannes (id, description, urgence, statut, date_declaration, date_cloture, equipement_id, declarant_id, technicien_id)
VALUES
  (1, 'Le PC ne demarre plus', 'HAUTE', 'EN_COURS', DATE_SUB(NOW(6), INTERVAL 6 HOUR), NULL, 1, 3, 2),
  (2, 'Lenteur importante au demarrage', 'MOYENNE', 'REPAREE', DATE_SUB(NOW(6), INTERVAL 12 DAY), NULL, 3, 3, 2),
  (3, 'Bourrage papier recurrent', 'FAIBLE', 'DECLAREE', DATE_SUB(NOW(6), INTERVAL 2 DAY), NULL, 4, 3, NULL),
  (4, 'Perte de connectivite intermittente', 'HAUTE', 'EN_ATTENTE_PIECE', DATE_SUB(NOW(6), INTERVAL 5 DAY), NULL, 5, 1, 2),
  (5, 'Ecran scintille apres 10 minutes', 'MOYENNE', 'CLOTUREE', DATE_SUB(NOW(6), INTERVAL 1 MONTH), DATE_SUB(NOW(6), INTERVAL 20 DAY), 7, 3, 2)
ON DUPLICATE KEY UPDATE urgence = VALUES(urgence), statut = VALUES(statut), technicien_id = VALUES(technicien_id), date_cloture = VALUES(date_cloture);

INSERT INTO reparations (id, description, date_debut, date_fin, cout_total, note_satisfaction, panne_id, technicien_id)
VALUES
  (1, 'Diagnostic carte mere et test alimentation', DATE_SUB(NOW(6), INTERVAL 5 HOUR), NULL, 350.00, 4, 1, 2),
  (2, 'Remplacement SSD et nettoyage systeme', DATE_SUB(NOW(6), INTERVAL 11 DAY), DATE_SUB(NOW(6), INTERVAL 10 DAY), 780.00, 5, 2, 2),
  (3, 'Remplacement cable video ecran', DATE_SUB(NOW(6), INTERVAL 25 DAY), DATE_SUB(NOW(6), INTERVAL 20 DAY), 180.00, 4, 5, 2)
ON DUPLICATE KEY UPDATE cout_total = VALUES(cout_total), note_satisfaction = VALUES(note_satisfaction), date_fin = VALUES(date_fin);

INSERT INTO mouvements_stock (id, type_mouvement, quantite, date_heure, motif, piece_id, reparation_id, utilisateur_id)
VALUES
  (1, 'ENTREE', 10, DATE_SUB(NOW(6), INTERVAL 20 DAY), 'Stock initial', 1, NULL, 1),
  (2, 'CONSOMMATION', 1, DATE_SUB(NOW(6), INTERVAL 10 DAY), 'Reparation PC003', 2, 2, 2),
  (3, 'SORTIE', 5, DATE_SUB(NOW(6), INTERVAL 3 DAY), 'Remplacement cables salle reunion', 4, NULL, 2)
ON DUPLICATE KEY UPDATE quantite = VALUES(quantite), motif = VALUES(motif);

INSERT INTO prets (id, date_depart, date_retour_prevue, date_retour_reelle, motif, statut, equipement_id, employe_id, valideur_id)
VALUES
  (1, DATE_ADD(CURRENT_DATE, INTERVAL 0 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), NULL, 'Presentation projet', 'EN_ATTENTE', 2, 3, NULL),
  (2, DATE_SUB(CURRENT_DATE, INTERVAL 4 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), NULL, 'Mission externe', 'VALIDE', 6, 2, 1),
  (3, DATE_SUB(CURRENT_DATE, INTERVAL 9 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), NULL, 'Inventaire agence', 'EN_RETARD', 4, 1, 1),
  (4, DATE_SUB(CURRENT_DATE, INTERVAL 22 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 12 DAY), 'Formation interne', 'CLOTURE', 7, 1, 1)
ON DUPLICATE KEY UPDATE statut = VALUES(statut), date_retour_reelle = VALUES(date_retour_reelle), valideur_id = VALUES(valideur_id);

INSERT INTO historiques_equipement (id, evenement, date_heure, details, equipement_id)
VALUES
  (1, 'EQUIPEMENT_ENREGISTRE', DATE_SUB(NOW(6), INTERVAL 30 DAY), 'Creation fiche equipement PC001', 1),
  (2, 'PANNE_DECLAREE', DATE_SUB(NOW(6), INTERVAL 6 HOUR), 'Le PC ne demarre plus', 1),
  (3, 'PRET_VALIDE', DATE_SUB(NOW(6), INTERVAL 4 DAY), 'Pret valide pour Sara Bennani', 6),
  (4, 'REPARATION_CLOTUREE', DATE_SUB(NOW(6), INTERVAL 20 DAY), 'Ecran repare et cloture', 7)
ON DUPLICATE KEY UPDATE evenement = VALUES(evenement), details = VALUES(details);
