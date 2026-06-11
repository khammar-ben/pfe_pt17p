package com.example.demo.web;

import com.example.demo.domain.Employe;
import com.example.demo.domain.Fournisseur;
import com.example.demo.domain.ServiceDepartement;
import com.example.demo.repository.EmployeRepository;
import com.example.demo.repository.FournisseurRepository;
import com.example.demo.repository.ServiceDepartementRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReferentielController {
    private final ServiceDepartementRepository services;
    private final EmployeRepository employes;
    private final FournisseurRepository fournisseurs;

    public ReferentielController(ServiceDepartementRepository services, EmployeRepository employes,
            FournisseurRepository fournisseurs) {
        this.services = services;
        this.employes = employes;
        this.fournisseurs = fournisseurs;
    }

    @GetMapping("/services")
    List<ServiceDepartement> services() {
        return services.findAll();
    }

    @PostMapping("/services")
    ServiceDepartement createService(@RequestBody ServiceDepartement service) {
        return services.save(service);
    }

    @GetMapping("/employes")
    List<Employe> employes() {
        return employes.findAll();
    }

    @PostMapping("/employes")
    Employe createEmploye(@RequestBody Employe employe) {
        return employes.save(employe);
    }

    @GetMapping("/fournisseurs")
    List<Fournisseur> fournisseurs() {
        return fournisseurs.findAll();
    }

    @PostMapping("/fournisseurs")
    Fournisseur createFournisseur(@RequestBody Fournisseur fournisseur) {
        return fournisseurs.save(fournisseur);
    }
}
