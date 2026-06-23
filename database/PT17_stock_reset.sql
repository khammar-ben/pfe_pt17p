USE pt17_db;

-- Reset propre du stock PT17.
-- Attention: ce script supprime les mouvements stock et les anciennes demandes d'equipement
-- afin de repartir avec un inventaire clair pour les tests/analyse.

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE mouvements_stock;

SET @demandes_exists = (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'demandes_equipement'
);
SET @truncate_demandes = IF(@demandes_exists = 1, 'TRUNCATE TABLE demandes_equipement', 'SELECT 1');
PREPARE truncate_demandes_stmt FROM @truncate_demandes;
EXECUTE truncate_demandes_stmt;
DEALLOCATE PREPARE truncate_demandes_stmt;

TRUNCATE TABLE pieces;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO fournisseurs (id, nom, contact, specialite, email)
VALUES
  (1, 'TechParts', '+212600000100', 'Pieces PC', 'contact@techparts.test'),
  (2, 'CasaNetworks', '+212600000200', 'Reseau et peripheriques', 'sales@casanetworks.test'),
  (3, 'Atlas Digital', '+212600000300', 'Postes utilisateurs', 'commande@atlasdigital.test'),
  (4, 'PrintOffice', '+212600000400', 'Impression et consommables', 'support@printoffice.test'),
  (5, 'SecureLink', '+212600000500', 'Accessoires et securite', 'contact@securelink.test')
ON DUPLICATE KEY UPDATE contact = VALUES(contact), specialite = VALUES(specialite), email = VALUES(email);

INSERT INTO pieces (id, reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
VALUES
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
  (38, 'PATCH-CAT6-0M5', 'Patch cable RJ45 Cat6 0.5m', 40, 15, 'Casier Reseau N2', 18.00, 'RECHANGE', 2);

INSERT INTO mouvements_stock (type_mouvement, quantite, date_heure, motif, piece_id, utilisateur_id)
SELECT 'ENTREE', quantite_stock, NOW(6), 'Initialisation stock propre PT17', id, 1
FROM pieces;
