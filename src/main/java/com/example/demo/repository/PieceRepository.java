package com.example.demo.repository;

import com.example.demo.domain.Piece;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PieceRepository extends JpaRepository<Piece, Long> {
    Optional<Piece> findByReferenceIgnoreCase(String reference);

    List<Piece> findByQuantiteStockLessThan(int seuil);

    @Query("select p from Piece p where p.quantiteStock < p.seuilMinimum")
    List<Piece> findStockCritique();

    @Query(value = """
            SELECT *
            FROM pieces
            WHERE UPPER(reference) LIKE CONCAT(UPPER(:prefix), '-%')
              AND quantite_stock > 0
            ORDER BY reference
            """, nativeQuery = true)
    List<Piece> findAvailableByReferencePrefix(@Param("prefix") String prefix);
}
