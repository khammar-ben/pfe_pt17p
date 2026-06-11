package com.example.demo.service;

import com.example.demo.domain.NiveauUrgence;
import com.example.demo.domain.Panne;
import com.example.demo.domain.Piece;
import com.example.demo.domain.Pret;
import com.example.demo.domain.RoleType;
import com.example.demo.domain.StatutPanne;
import com.example.demo.domain.StatutPret;
import com.example.demo.domain.Utilisateur;
import com.example.demo.repository.PanneRepository;
import com.example.demo.repository.PieceRepository;
import com.example.demo.repository.PretRepository;
import com.example.demo.repository.UtilisateurRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "app.notifications.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledNotificationService {
    private final PretRepository pretRepository;
    private final PanneRepository panneRepository;
    private final PieceRepository pieceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;

    public ScheduledNotificationService(
            PretRepository pretRepository,
            PanneRepository panneRepository,
            PieceRepository pieceRepository,
            UtilisateurRepository utilisateurRepository,
            NotificationService notificationService) {
        this.pretRepository = pretRepository;
        this.panneRepository = panneRepository;
        this.pieceRepository = pieceRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${app.notifications.cron.prets:0 0 8 * * *}")
    @Transactional
    public NotificationRunResult executerRappelsPrets() {
        int rappels = rappelerPretsAvantRetour();
        int retards = marquerPretsEnRetard();
        return new NotificationRunResult(rappels, retards, 0, 0);
    }

    @Scheduled(cron = "${app.notifications.cron.pannes:0 0 */4 * * *}")
    @Transactional(readOnly = true)
    public NotificationRunResult executerEscaladesPannes() {
        return new NotificationRunResult(0, 0, escaladerPannesHautes(), 0);
    }

    @Scheduled(cron = "${app.notifications.cron.stock:0 30 */6 * * *}")
    @Transactional(readOnly = true)
    public NotificationRunResult executerAlertesStock() {
        return new NotificationRunResult(0, 0, 0, alerterStockCritique());
    }

    @Transactional
    public NotificationRunResult executerTout() {
        NotificationRunResult prets = executerRappelsPrets();
        NotificationRunResult pannes = executerEscaladesPannes();
        NotificationRunResult stock = executerAlertesStock();
        return new NotificationRunResult(
                prets.rappelsPrets(),
                prets.pretsEnRetard(),
                pannes.escaladesPannes(),
                stock.alertesStock());
    }

    private int rappelerPretsAvantRetour() {
        LocalDate dateCible = LocalDate.now().plusDays(2);
        List<Pret> prets = pretRepository.findByStatutAndDateRetourPrevue(StatutPret.VALIDE, dateCible);
        int count = 0;
        for (Pret pret : prets) {
            String email = pret.getEmploye() == null ? null : pret.getEmploye().getEmail();
            String equipement = pret.getEquipement() == null ? "equipement" : pret.getEquipement().getNumSerie();
            if (notificationService.envoyer(
                    email,
                    "Rappel retour equipement",
                    "Le pret de l'equipement " + equipement + " doit etre retourne le " + dateCible + ".")) {
                count++;
            }
        }
        return count;
    }

    private int marquerPretsEnRetard() {
        List<Pret> prets = pretRepository.findByStatutAndDateRetourPrevueBefore(StatutPret.VALIDE, LocalDate.now());
        for (Pret pret : prets) {
            pret.setStatut(StatutPret.EN_RETARD);
        }
        pretRepository.saveAll(prets);
        return prets.size();
    }

    private int escaladerPannesHautes() {
        List<Panne> pannes = panneRepository.findByUrgenceAndDateDeclarationBeforeAndStatutNotIn(
                NiveauUrgence.HAUTE,
                LocalDateTime.now().minusHours(4),
                List.of(StatutPanne.REPAREE, StatutPanne.CLOTUREE));
        List<Utilisateur> destinataires = utilisateursParRoles(List.of(RoleType.ADMIN, RoleType.DIRECTEUR));
        int count = 0;
        for (Panne panne : pannes) {
            String equipement = panne.getEquipement() == null ? "equipement" : panne.getEquipement().getNumSerie();
            for (Utilisateur destinataire : destinataires) {
                if (notificationService.envoyer(
                        destinataire.getEmail(),
                        "Escalade panne haute",
                        "La panne #" + panne.getId() + " sur " + equipement + " est urgente depuis plus de 4 heures.")) {
                    count++;
                }
            }
        }
        return count;
    }

    private int alerterStockCritique() {
        List<Piece> pieces = pieceRepository.findStockCritique();
        if (pieces.isEmpty()) {
            return 0;
        }

        StringBuilder message = new StringBuilder("Pieces sous le seuil minimum:\n");
        for (Piece piece : pieces) {
            message.append("- ")
                    .append(piece.getReference())
                    .append(" / ")
                    .append(piece.getDesignation())
                    .append(": ")
                    .append(piece.getQuantiteStock())
                    .append(" < ")
                    .append(piece.getSeuilMinimum())
                    .append('\n');
        }

        int count = 0;
        for (Utilisateur destinataire : utilisateursParRoles(List.of(RoleType.ADMIN, RoleType.TECHNICIEN))) {
            if (notificationService.envoyer(destinataire.getEmail(), "Alerte stock critique", message.toString())) {
                count++;
            }
        }
        return count;
    }

    private List<Utilisateur> utilisateursParRoles(List<RoleType> roles) {
        return utilisateurRepository.findByRoleInAndActifTrue(roles).stream()
                .filter(utilisateur -> utilisateur.getEmail() != null && !utilisateur.getEmail().isBlank())
                .toList();
    }

    public record NotificationRunResult(
            int rappelsPrets,
            int pretsEnRetard,
            int escaladesPannes,
            int alertesStock) {
    }
}
