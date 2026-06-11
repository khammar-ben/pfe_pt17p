package com.example.demo.web;

import com.example.demo.domain.Utilisateur;
import com.example.demo.repository.UtilisateurRepository;
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
    private final PasswordEncoder passwordEncoder;

    public UtilisateurController(UtilisateurRepository utilisateurs, PasswordEncoder passwordEncoder) {
        this.utilisateurs = utilisateurs;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    List<Utilisateur> all() {
        return utilisateurs.findAll();
    }

    @PostMapping
    Utilisateur create(@RequestBody Utilisateur utilisateur) {
        utilisateur.setMotDePasse(passwordEncoder.encode(utilisateur.getMotDePasse()));
        return utilisateurs.save(utilisateur);
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
    Utilisateur resetPassword(@PathVariable Long id, @RequestBody PasswordRequest request) {
        Utilisateur utilisateur = utilisateurs.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable: " + id));
        utilisateur.setMotDePasse(passwordEncoder.encode(request.motDePasse()));
        utilisateur.setTentativesEchec(0);
        utilisateur.setActif(true);
        return utilisateurs.save(utilisateur);
    }

    record PasswordRequest(String motDePasse) {
    }
}
