package com.example.demo.repository;

import com.example.demo.domain.Equipement;
import com.example.demo.domain.StatutEquipement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipementRepository extends JpaRepository<Equipement, Long> {
    long countByStatut(StatutEquipement statut);
}
