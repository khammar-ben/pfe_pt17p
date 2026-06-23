package com.example.demo.repository;

import com.example.demo.domain.Pret;
import com.example.demo.domain.StatutPret;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PretRepository extends JpaRepository<Pret, Long> {
    long countByStatut(StatutPret statut);

    List<Pret> findByStatutAndDateRetourPrevue(StatutPret statut, LocalDate dateRetourPrevue);

    List<Pret> findByStatutAndDateRetourPrevueBefore(StatutPret statut, LocalDate dateRetourPrevue);

    @Query("select p from Pret p where p.employe in (select u.employe from Utilisateur u where u.login = :login)")
    List<Pret> findByUtilisateurLogin(@Param("login") String login);
}
