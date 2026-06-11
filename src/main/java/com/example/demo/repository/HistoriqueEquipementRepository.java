package com.example.demo.repository;

import com.example.demo.domain.HistoriqueEquipement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoriqueEquipementRepository extends JpaRepository<HistoriqueEquipement, Long> {
    List<HistoriqueEquipement> findByEquipementIdOrderByDateHeureDesc(Long equipementId);
}
