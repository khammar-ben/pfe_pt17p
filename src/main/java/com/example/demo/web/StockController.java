package com.example.demo.web;

import com.example.demo.domain.MouvementStock;
import com.example.demo.domain.Piece;
import com.example.demo.domain.TypeMouvementStock;
import com.example.demo.repository.MouvementStockRepository;
import com.example.demo.repository.PieceRepository;
import com.example.demo.service.StockService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
public class StockController {
    private final PieceRepository pieces;
    private final MouvementStockRepository mouvements;
    private final StockService service;

    public StockController(PieceRepository pieces, MouvementStockRepository mouvements, StockService service) {
        this.pieces = pieces;
        this.mouvements = mouvements;
        this.service = service;
    }

    @GetMapping("/pieces")
    List<Piece> pieces() {
        return pieces.findAll();
    }

    @PostMapping("/pieces")
    Piece createPiece(@RequestBody Piece piece) {
        return pieces.save(piece);
    }

    @GetMapping("/mouvements")
    List<MouvementStock> mouvements() {
        return mouvements.findAll();
    }

    @PostMapping("/mouvements")
    MouvementStock mouvement(@RequestBody MouvementRequest request) {
        return service.enregistrerMouvement(request.pieceId(), request.quantite(), request.typeMouvement(),
                request.motif(), request.reparationId(), request.utilisateurId());
    }

    record MouvementRequest(Long pieceId, int quantite, TypeMouvementStock typeMouvement,
            String motif, Long reparationId, Long utilisateurId) {
    }
}
