package com.example.demo.web;

import com.example.demo.domain.NotificationInterne;
import com.example.demo.service.NotificationInterneService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app-notifications")
public class NotificationInterneController {
    private final NotificationInterneService service;

    public NotificationInterneController(NotificationInterneService service) {
        this.service = service;
    }

    @GetMapping
    List<NotificationInterne> all(Authentication authentication) {
        return service.pourUtilisateur(authentication.getName());
    }

    @PutMapping("/{id}/lue")
    NotificationInterne marquerLue(@PathVariable Long id, Authentication authentication) {
        return service.marquerLue(id, authentication.getName());
    }

    @PutMapping("/{id}/affecter")
    NotificationInterne affecter(@PathVariable Long id, Authentication authentication) {
        return service.affecterEquipement(id, authentication.getName());
    }

    @PutMapping("/{id}/done")
    NotificationInterne done(@PathVariable Long id, Authentication authentication) {
        return service.terminerEquipement(id, authentication.getName());
    }
}
