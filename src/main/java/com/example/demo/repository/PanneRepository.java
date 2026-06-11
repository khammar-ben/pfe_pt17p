package com.example.demo.repository;

import com.example.demo.domain.NiveauUrgence;
import com.example.demo.domain.Panne;
import com.example.demo.domain.StatutPanne;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PanneRepository extends JpaRepository<Panne, Long> {
    long countByStatutNot(StatutPanne statut);

    List<Panne> findByUrgenceAndDateDeclarationBeforeAndStatutNotIn(
            NiveauUrgence urgence,
            LocalDateTime dateDeclaration,
            Collection<StatutPanne> statutsExclus);
}
