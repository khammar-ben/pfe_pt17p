package com.example.demo.repository;

import com.example.demo.domain.DemandeEquipement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandeEquipementRepository extends JpaRepository<DemandeEquipement, Long> {
    List<DemandeEquipement> findByEmployeIdOrderByDateDemandeDesc(Long employeId);

    List<DemandeEquipement> findAllByOrderByDateDemandeDesc();
}
