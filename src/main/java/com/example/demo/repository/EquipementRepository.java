package com.example.demo.repository;

import com.example.demo.domain.Equipement;
import com.example.demo.domain.StatutEquipement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipementRepository extends JpaRepository<Equipement, Long> {
    long countByStatut(StatutEquipement statut);

    Optional<Equipement> findByNumSerie(String numSerie);

    @Query("select e from Equipement e where e.employe in (select u.employe from Utilisateur u where u.login = :login)")
    List<Equipement> findByUtilisateurLogin(@Param("login") String login);
}
