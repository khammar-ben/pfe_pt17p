# PT17 MySQL setup

This project keeps H2 as the default local database. Use the `mysql` Spring profile when you want to run against MySQL 8.

## Create the database

From PowerShell, if `mysql` is available in your PATH:

```powershell
cd C:\Users\LAGZOULI\IdeaProjects\demo
mysql -u root -p < database\PT17_script.sql
```

If `mysql` is not in PATH, open MySQL Workbench and run `database/PT17_script.sql`.

## Run the project with MySQL

```powershell
cd C:\Users\LAGZOULI\IdeaProjects\demo
$env:MYSQL_USER="root"
$env:MYSQL_PASSWORD="your_mysql_password"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

Le profil MySQL utilise `JPA_DDL_AUTO=update` par defaut. Pour forcer la validation stricte du schema:

```powershell
$env:JPA_DDL_AUTO="validate"
```

The default JDBC URL is:

```text
jdbc:mysql://localhost:3306/pt17_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
```

Override it when needed:

```powershell
$env:MYSQL_URL="jdbc:mysql://localhost:3306/pt17_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
```

## Test endpoints

```text
http://localhost:9090/api/dashboard
http://localhost:9090/api/equipements
http://localhost:9090/api/pannes
http://localhost:9090/api/stock/pieces
```
