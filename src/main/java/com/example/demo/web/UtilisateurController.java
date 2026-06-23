package com.example.demo.web;

import com.example.demo.domain.Employe;
import com.example.demo.domain.RoleType;
import com.example.demo.domain.Utilisateur;
import com.example.demo.repository.EmployeRepository;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.service.NotificationService;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurController {
    private final UtilisateurRepository utilisateurs;
    private final EmployeRepository employes;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final SecureRandom secureRandom = new SecureRandom();
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$";

    public UtilisateurController(UtilisateurRepository utilisateurs, EmployeRepository employes,
            PasswordEncoder passwordEncoder, NotificationService notificationService) {
        this.utilisateurs = utilisateurs;
        this.employes = employes;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @GetMapping
    List<Utilisateur> all() {
        return utilisateurs.findAll();
    }

    @PostMapping
    CredentialsResponse create(@RequestBody CreateAccountRequest request) {
        Employe employe = employes.findById(request.employeId())
                .orElseThrow(() -> new IllegalArgumentException("Employe introuvable: " + request.employeId()));
        if (utilisateurs.existsByEmployeId(employe.getId())) {
            throw new IllegalArgumentException("Cet employe possede deja un compte");
        }

        String login = uniqueLogin(employe);
        String temporaryPassword = generatePassword();
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setLogin(login);
        utilisateur.setMotDePasse(passwordEncoder.encode(temporaryPassword));
        utilisateur.setEmail(cleanEmail(employe.getEmail()));
        utilisateur.setRole(RoleType.EMPLOYE);
        utilisateur.setEmploye(employe);
        utilisateur.setActif(true);
        Utilisateur saved = utilisateurs.save(utilisateur);
        boolean emailEnvoye = envoyerIdentifiants(saved, temporaryPassword, false);
        return credentials(saved, temporaryPassword, emailEnvoye);
    }

    @PutMapping("/{id}")
    Utilisateur update(@PathVariable Long id, @RequestBody Utilisateur input) {
        Utilisateur utilisateur = utilisateurs.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable: " + id));
        utilisateur.setLogin(input.getLogin());
        utilisateur.setEmail(input.getEmail());
        utilisateur.setRole(input.getRole());
        utilisateur.setActif(input.isActif());
        return utilisateurs.save(utilisateur);
    }

    @PutMapping("/{id}/desactiver")
    Utilisateur desactiver(@PathVariable Long id) {
        Utilisateur utilisateur = utilisateurs.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable: " + id));
        utilisateur.setActif(false);
        return utilisateurs.save(utilisateur);
    }

    @PutMapping("/{id}/reactiver")
    Utilisateur reactiver(@PathVariable Long id) {
        Utilisateur utilisateur = utilisateurs.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable: " + id));
        utilisateur.setActif(true);
        utilisateur.setTentativesEchec(0);
        return utilisateurs.save(utilisateur);
    }

    @PutMapping("/{id}/mot-de-passe")
    CredentialsResponse resetPassword(@PathVariable Long id) {
        Utilisateur utilisateur = utilisateurs.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable: " + id));
        String temporaryPassword = generatePassword();
        utilisateur.setMotDePasse(passwordEncoder.encode(temporaryPassword));
        utilisateur.setTentativesEchec(0);
        utilisateur.setActif(true);
        Utilisateur saved = utilisateurs.save(utilisateur);
        boolean emailEnvoye = envoyerIdentifiants(saved, temporaryPassword, true);
        return credentials(saved, temporaryPassword, emailEnvoye);
    }

    private CredentialsResponse credentials(Utilisateur utilisateur, String temporaryPassword, boolean emailEnvoye) {
        String employeNom = utilisateur.getEmploye() == null
                ? utilisateur.getLogin()
                : utilisateur.getEmploye().getPrenom() + " " + utilisateur.getEmploye().getNom();
        return new CredentialsResponse(
                utilisateur.getId(),
                employeNom.trim(),
                utilisateur.getLogin(),
                temporaryPassword,
                utilisateur.getEmail(),
                utilisateur.getRole(),
                emailEnvoye);
    }

    private boolean envoyerIdentifiants(Utilisateur utilisateur, String temporaryPassword, boolean reset) {
        String nom = utilisateur.getEmploye() == null
                ? utilisateur.getLogin()
                : utilisateur.getEmploye().getPrenom() + " " + utilisateur.getEmploye().getNom();
        String sujet = reset ? "Nouveau mot de passe PT17" : "Votre compte PT17";
        String message = """
                Bonjour %s,

                %s

                Login : %s
                Mot de passe temporaire : %s

                Pour votre securite, gardez ces informations confidentielles.

                Cordialement,
                Support PT17
                """.formatted(
                nom.trim(),
                reset
                        ? "Un nouveau mot de passe temporaire a ete genere pour votre compte."
                        : "Votre compte d'acces a l'application PT17 a ete cree.",
                utilisateur.getLogin(),
                temporaryPassword);
        return notificationService.envoyer(utilisateur.getEmail(), sujet, message);
    }

    private String cleanEmail(String email) {
        return email == null ? null : email.trim();
    }

    private String uniqueLogin(Employe employe) {
        String base = normalize(employe.getPrenom()) + "." + normalize(employe.getNom());
        base = base.replaceAll("^\\.+|\\.+$", "");
        if (base.isBlank()) {
            base = "employe" + employe.getId();
        }
        String candidate = base;
        int suffix = 2;
        while (utilisateurs.findByLogin(candidate).isPresent()) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", ".");
    }

    private String generatePassword() {
        StringBuilder password = new StringBuilder(12);
        for (int index = 0; index < 12; index++) {
            password.append(PASSWORD_CHARS.charAt(secureRandom.nextInt(PASSWORD_CHARS.length())));
        }
        return password.toString();
    }

    record CreateAccountRequest(Long employeId) {
    }

    record CredentialsResponse(Long id, String employe, String login, String motDePasseTemporaire,
            String email, RoleType role, boolean emailEnvoye) {
    }
}
