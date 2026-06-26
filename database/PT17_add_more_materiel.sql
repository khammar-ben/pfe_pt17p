USE pt17_db;

-- Catalogue materiel supplementaire pour l'onboarding pack.
-- Script idempotent: chaque reference s'ajoute seulement si elle n'existe pas.

INSERT INTO fournisseurs (nom, contact, specialite, email)
SELECT 'WorkStation Pro', '+212600000600', 'Postes premium et stations', 'sales@workstationpro.test'
WHERE NOT EXISTS (SELECT 1 FROM fournisseurs WHERE nom = 'WorkStation Pro');

INSERT INTO fournisseurs (nom, contact, specialite, email)
SELECT 'ScreenHub', '+212600000700', 'Ecrans et affichage', 'contact@screenhub.test'
WHERE NOT EXISTS (SELECT 1 FROM fournisseurs WHERE nom = 'ScreenHub');

INSERT INTO fournisseurs (nom, contact, specialite, email)
SELECT 'OfficeGear', '+212600000800', 'Accessoires bureau', 'orders@officegear.test'
WHERE NOT EXISTS (SELECT 1 FROM fournisseurs WHERE nom = 'OfficeGear');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'PC-LEN-X1-CARBON', 'Lenovo ThinkPad X1 Carbon i7 16GB 1TB', 6, 2, 'Zone Materiel M1', 18500.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'WorkStation Pro'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'PC-LEN-X1-CARBON');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'PC-HP-1040-G9', 'HP EliteBook 1040 G9 i7 16GB 512GB', 7, 2, 'Zone Materiel M1', 16800.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'WorkStation Pro'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'PC-HP-1040-G9');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'PC-DELL-XPS13', 'Dell XPS 13 i7 16GB 1TB', 5, 2, 'Zone Materiel M1', 17600.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'WorkStation Pro'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'PC-DELL-XPS13');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'PC-ASUS-ZENBOOK', 'Asus ZenBook 14 OLED i7 16GB 512GB', 8, 2, 'Zone Materiel M2', 13900.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'WorkStation Pro'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'PC-ASUS-ZENBOOK');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'PC-ACER-TRAVELMATE', 'Acer TravelMate P2 i5 16GB 512GB', 12, 4, 'Zone Materiel M2', 7200.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'Atlas Digital'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'PC-ACER-TRAVELMATE');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'DESK-DELL-OPTIPLEX', 'Dell OptiPlex 7010 Micro i5 16GB 512GB', 9, 3, 'Zone Materiel M3', 7600.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'Atlas Digital'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'DESK-DELL-OPTIPLEX');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'DESK-HP-MINI-400', 'HP Pro Mini 400 G9 i5 16GB 512GB', 10, 3, 'Zone Materiel M3', 6900.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'Atlas Digital'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'DESK-HP-MINI-400');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'ECRAN-DELL-27-QHD', 'Ecran Dell P2723D 27 pouces QHD', 12, 4, 'Rayon Ecrans E2', 3400.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'ScreenHub'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'ECRAN-DELL-27-QHD');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'ECRAN-LG-29-ULTRA', 'Ecran LG UltraWide 29 pouces', 6, 2, 'Rayon Ecrans E2', 3900.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'ScreenHub'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'ECRAN-LG-29-ULTRA');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'ECRAN-SAMSUNG-24', 'Ecran Samsung 24 pouces IPS', 18, 5, 'Rayon Ecrans E1', 1750.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'ScreenHub'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'ECRAN-SAMSUNG-24');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'CLAVIER-LOGI-MX', 'Clavier Logitech MX Keys AZERTY', 14, 4, 'Casier Accessoires A1', 890.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'OfficeGear'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'CLAVIER-LOGI-MX');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'CLAVIER-DELL-KB216', 'Clavier Dell KB216 AZERTY', 26, 8, 'Casier Accessoires A1', 160.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'OfficeGear'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'CLAVIER-DELL-KB216');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'SOURIS-LOGI-MX-MASTER', 'Souris Logitech MX Master 3S', 12, 4, 'Casier Accessoires A1', 820.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'OfficeGear'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'SOURIS-LOGI-MX-MASTER');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'SOURIS-DELL-MS116', 'Souris Dell MS116 USB', 30, 10, 'Casier Accessoires A1', 95.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'OfficeGear'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'SOURIS-DELL-MS116');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'CASQUE-LOGI-H390', 'Casque Logitech H390 USB', 18, 5, 'Casier Accessoires A2', 420.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'OfficeGear'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'CASQUE-LOGI-H390');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'CASQUE-JABRA-65', 'Casque Jabra Evolve2 65 Bluetooth', 9, 3, 'Casier Accessoires A2', 1650.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'OfficeGear'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'CASQUE-JABRA-65');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'DOCK-HP-G5', 'Station accueil HP USB-C Dock G5', 10, 3, 'Casier Accessoires A3', 1450.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'OfficeGear'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'DOCK-HP-G5');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'WEBCAM-LOGI-C920', 'Webcam Logitech C920 Full HD', 11, 3, 'Casier Accessoires A5', 780.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'OfficeGear'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'WEBCAM-LOGI-C920');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'IMP-BROTHER-HL-L2350', 'Imprimante Brother HL-L2350DW', 4, 2, 'Zone Impression I1', 2600.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'PrintOffice'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'IMP-BROTHER-HL-L2350');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'IMP-EPSON-WF-PRO', 'Imprimante Epson WorkForce Pro', 5, 2, 'Zone Impression I1', 4200.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'PrintOffice'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'IMP-EPSON-WF-PRO');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'SW-TP-LINK-24G', 'Switch TP-Link JetStream 24 ports Gigabit', 7, 2, 'Rack Reseau R1', 2300.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'CasaNetworks'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'SW-TP-LINK-24G');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'ROUTEUR-MIKROTIK-HAP', 'Routeur MikroTik hAP ax3', 8, 2, 'Rack Reseau R2', 1450.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'CasaNetworks'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'ROUTEUR-MIKROTIK-HAP');

INSERT INTO pieces (reference, designation, quantite_stock, seuil_minimum, localisation, prix_unitaire, categorie_usage, fournisseur_id)
SELECT 'AP-TP-LINK-EAP610', 'Point acces TP-Link Omada EAP610', 12, 4, 'Rack Reseau R2', 1250.00, 'MATERIEL', f.id
FROM fournisseurs f WHERE f.nom = 'CasaNetworks'
AND NOT EXISTS (SELECT 1 FROM pieces p WHERE p.reference = 'AP-TP-LINK-EAP610');

INSERT INTO mouvements_stock (type_mouvement, quantite, date_heure, motif, piece_id, utilisateur_id)
SELECT 'ENTREE', p.quantite_stock, NOW(6), 'Ajout catalogue materiel PT17', p.id, 1
FROM pieces p
WHERE p.reference IN (
  'PC-LEN-X1-CARBON','PC-HP-1040-G9','PC-DELL-XPS13','PC-ASUS-ZENBOOK','PC-ACER-TRAVELMATE',
  'DESK-DELL-OPTIPLEX','DESK-HP-MINI-400','ECRAN-DELL-27-QHD','ECRAN-LG-29-ULTRA','ECRAN-SAMSUNG-24',
  'CLAVIER-LOGI-MX','CLAVIER-DELL-KB216','SOURIS-LOGI-MX-MASTER','SOURIS-DELL-MS116',
  'CASQUE-LOGI-H390','CASQUE-JABRA-65','DOCK-HP-G5','WEBCAM-LOGI-C920',
  'IMP-BROTHER-HL-L2350','IMP-EPSON-WF-PRO','SW-TP-LINK-24G','ROUTEUR-MIKROTIK-HAP','AP-TP-LINK-EAP610'
)
AND NOT EXISTS (
  SELECT 1 FROM mouvements_stock m
  WHERE m.piece_id = p.id
    AND m.motif = 'Ajout catalogue materiel PT17'
);
