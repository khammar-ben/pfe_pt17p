package com.example.demo.web;

import com.example.demo.domain.Utilisateur;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.service.JwtService;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UtilisateurRepository utilisateurs;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UtilisateurRepository utilisateurs, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.utilisateurs = utilisateurs;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    Map<String, Object> login(@RequestBody LoginRequest request) {
        Utilisateur utilisateur = utilisateurs.findByLogin(request.login())
                .filter(Utilisateur::isActif)
                .orElseThrow(() -> new IllegalArgumentException("Login ou mot de passe invalide"));
        if (!passwordMatches(request.motDePasse(), utilisateur)) {
            enregistrerEchec(utilisateur);
            throw new IllegalArgumentException(utilisateur.isActif()
                    ? "Login ou mot de passe invalide"
                    : "Compte bloque apres 5 tentatives invalides");
        }
        if (utilisateur.getTentativesEchec() > 0) {
            utilisateur.setTentativesEchec(0);
            utilisateurs.save(utilisateur);
        }
        return Map.of(
                "token", jwtService.generateToken(utilisateur),
                "type", "Bearer",
                "userId", utilisateur.getId(),
                "login", utilisateur.getLogin(),
                "role", utilisateur.getRole(),
                "employeId", utilisateur.getEmploye() == null ? "" : utilisateur.getEmploye().getId()
        );
    }

    @GetMapping("/me")
    Map<String, Object> me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Utilisateur non authentifie");
        }
        Utilisateur utilisateur = utilisateurs.findByLogin(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        return Map.of(
                "userId", utilisateur.getId(),
                "login", utilisateur.getLogin(),
                "role", utilisateur.getRole(),
                "employeId", utilisateur.getEmploye() == null ? "" : utilisateur.getEmploye().getId()
        );
    }

    private boolean passwordMatches(String rawPassword, Utilisateur utilisateur) {
        String storedPassword = utilisateur.getMotDePasse();
        if (storedPassword != null && storedPassword.startsWith("$2")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        if (storedPassword != null && storedPassword.equals(rawPassword)) {
            utilisateur.setMotDePasse(passwordEncoder.encode(rawPassword));
            utilisateurs.save(utilisateur);
            return true;
        }
        return false;
    }

    private void enregistrerEchec(Utilisateur utilisateur) {
        utilisateur.setTentativesEchec(utilisateur.getTentativesEchec() + 1);
        if (utilisateur.getTentativesEchec() >= 5) {
            utilisateur.setActif(false);
        }
        utilisateurs.save(utilisateur);
    }

    record LoginRequest(String login, String motDePasse) {
    }
}
