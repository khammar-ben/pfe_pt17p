package com.example.demo.repository;

import com.example.demo.domain.NiveauUrgence;
import com.example.demo.domain.Panne;
import com.example.demo.domain.StatutPanne;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PanneRepository extends JpaRepository<Panne, Long> {
    long countByStatutNot(StatutPanne statut);

    List<Panne> findByUrgenceAndDateDeclarationBeforeAndStatutNotIn(
            NiveauUrgence urgence,
            LocalDateTime dateDeclaration,
            Collection<StatutPanne> statutsExclus);

    List<Panne> findByDeclarantLogin(String login);

    List<Panne> findByTechnicienLogin(String login);

    @Query("select p from Panne p where p.technicien.login = :login or (p.technicien is null and p.statut = :statut)")
    List<Panne> findForTechnicien(@Param("login") String login, @Param("statut") StatutPanne statut);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Panne p where p.id = :id")
    Optional<Panne> findByIdForUpdate(@Param("id") Long id);
}
