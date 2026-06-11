package com.example.demo.repository;

import com.example.demo.domain.Piece;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PieceRepository extends JpaRepository<Piece, Long> {
    List<Piece> findByQuantiteStockLessThan(int seuil);

    @Query("select p from Piece p where p.quantiteStock < p.seuilMinimum")
    List<Piece> findStockCritique();
}
