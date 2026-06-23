package com.example.demo.service;

import com.example.demo.domain.MouvementStock;
import com.example.demo.domain.Piece;
import com.example.demo.domain.Reparation;
import com.example.demo.domain.TypeMouvementStock;
import com.example.demo.domain.Utilisateur;
import com.example.demo.repository.MouvementStockRepository;
import com.example.demo.repository.PieceRepository;
import com.example.demo.repository.ReparationRepository;
import com.example.demo.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {
    private final PieceRepository pieces;
    private final MouvementStockRepository mouvements;
    private final ReparationRepository reparations;
    private final UtilisateurRepository utilisateurs;

    public StockService(PieceRepository pieces, MouvementStockRepository mouvements,
            ReparationRepository reparations, UtilisateurRepository utilisateurs) {
        this.pieces = pieces;
        this.mouvements = mouvements;
        this.reparations = reparations;
        this.utilisateurs = utilisateurs;
    }

    @Transactional
    public MouvementStock enregistrerMouvement(Long pieceId, int quantite, TypeMouvementStock type,
            String motif, String nouvelleLocalisation, Long reparationId, Long utilisateurId) {
        Piece piece = pieces.findById(pieceId)
                .orElseThrow(() -> new NotFoundException("Piece introuvable: " + pieceId));
        if (type == TypeMouvementStock.TRANSFERT) {
            if (nouvelleLocalisation == null || nouvelleLocalisation.isBlank()) {
                throw new IllegalArgumentException("La nouvelle localisation est obligatoire");
            }
            String ancienneLocalisation = piece.getLocalisation() == null || piece.getLocalisation().isBlank()
                    ? "Non definie"
                    : piece.getLocalisation();
            String destination = nouvelleLocalisation.trim();
            if (ancienneLocalisation.equalsIgnoreCase(destination)) {
                throw new IllegalArgumentException("La nouvelle localisation doit etre differente");
            }
            piece.setLocalisation(destination);
            motif = (motif == null || motif.isBlank() ? "Transfert de stock" : motif)
                    + " : " + ancienneLocalisation + " -> " + destination;
            quantite = 0;
        } else if (type == TypeMouvementStock.ENTREE) {
            if (quantite <= 0) {
                throw new IllegalArgumentException("La quantite doit etre superieure a zero");
            }
            piece.setQuantiteStock(piece.getQuantiteStock() + quantite);
        } else {
            if (quantite <= 0) {
                throw new IllegalArgumentException("La quantite doit etre superieure a zero");
            }
            if (piece.getQuantiteStock() < quantite) {
                throw new IllegalArgumentException("Stock insuffisant pour " + piece.getReference());
            }
            piece.setQuantiteStock(piece.getQuantiteStock() - quantite);
        }
        pieces.save(piece);

        MouvementStock mouvement = new MouvementStock();
        mouvement.setPiece(piece);
        mouvement.setQuantite(quantite);
        mouvement.setTypeMouvement(type);
        mouvement.setMotif(motif);
        Reparation reparation = reparationId == null ? null : reparations.findById(reparationId).orElse(null);
        Utilisateur utilisateur = utilisateurId == null ? null : utilisateurs.findById(utilisateurId).orElse(null);
        mouvement.setReparation(reparation);
        mouvement.setUtilisateur(utilisateur);
        return mouvements.save(mouvement);
    }
}
