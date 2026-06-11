# PT17 - Gestion du parc informatique

Application Spring Boot 3.2 + React pour la gestion des equipements, pannes, reparations, stock, prets, utilisateurs, notifications, audit et rapports CSV.

## Prerequis

- Java 17 LTS
- Node.js 18+
- MySQL 8.0 si vous voulez utiliser le profil `mysql`

## Backend

Mode H2 en memoire:

```powershell
.\mvnw.cmd spring-boot:run
```

Mode MySQL:

```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:MYSQL_URL="jdbc:mysql://localhost:3306/pt17_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:MYSQL_USER="root"
$env:MYSQL_PASSWORD="votre_mot_de_passe"
.\mvnw.cmd spring-boot:run
```

Par defaut, le profil MySQL utilise `JPA_DDL_AUTO=update` pour creer/mettre a jour les tables au premier lancement. Pour verifier un schema deja pret sans modification automatique:

```powershell
$env:JPA_DDL_AUTO="validate"
```

API: `http://localhost:9090`

## Frontend

```powershell
cd frontend
npm install
npm run dev
```

Interface: `http://127.0.0.1:5173`

## Comptes par defaut

- Admin: `admin` / `admin`
- Technicien: `tech` / `tech`
- Employe: `employe` / `employe`

## Notifications mail

Sans SMTP, les notifications sont journalisees dans l'audit. Pour envoyer les emails, configurez:

```powershell
$env:SPRING_MAIL_HOST="smtp.example.com"
$env:SPRING_MAIL_PORT="587"
$env:SPRING_MAIL_USERNAME="user@example.com"
$env:SPRING_MAIL_PASSWORD="mot_de_passe"
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH="true"
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE="true"
```

Execution manuelle depuis l'interface: onglet `Audit`, bouton `Executer notifications`.

## Tests et build

Backend:

```powershell
.\mvnw.cmd test
```

Frontend:

```powershell
cd frontend
npm run build
```

## Fonctionnalites principales

- JWT login avec roles `ADMIN`, `TECHNICIEN`, `EMPLOYE`, `DIRECTEUR`
- Dashboard KPI et graphiques
- CRUD equipements, pannes, stock, reparations, prets, utilisateurs, fournisseurs
- Workflow prets: demande en attente, validation/refus admin, retour
- Blocage automatique d'un compte apres 5 tentatives de connexion invalides
- Reset mot de passe, activation/desactivation utilisateur
- Consommation de pieces lors d'une reparation avec mouvement stock automatique
- Upload photos pour equipements et pannes
- Notifications planifiees pour prets, pannes urgentes et stock critique
- Audit logs consultables par admin
- Exports CSV pour equipements, stock et pannes
