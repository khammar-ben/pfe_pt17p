package com.example.demo.repository;

import com.example.demo.domain.Pret;
import com.example.demo.domain.StatutPret;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PretRepository extends JpaRepository<Pret, Long> {
    long countByStatut(StatutPret statut);

    List<Pret> findByStatutAndDateRetourPrevue(StatutPret statut, LocalDate dateRetourPrevue);

    List<Pret> findByStatutAndDateRetourPrevueBefore(StatutPret statut, LocalDate dateRetourPrevue);
}
