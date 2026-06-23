package com.example.demo.repository;

import com.example.demo.domain.Reparation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReparationRepository extends JpaRepository<Reparation, Long> {
    List<Reparation> findByTechnicienLogin(String login);

    List<Reparation> findByPanneDeclarantLogin(String login);

    boolean existsByPanneIdAndDateFinIsNull(Long panneId);
}
