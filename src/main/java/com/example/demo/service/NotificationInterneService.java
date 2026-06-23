package com.example.demo.service;

import com.example.demo.domain.NotificationInterne;
import com.example.demo.domain.RoleType;
import com.example.demo.domain.StatutEquipement;
import com.example.demo.domain.StatutNotification;
import com.example.demo.domain.Utilisateur;
import com.example.demo.repository.EquipementRepository;
import com.example.demo.repository.NotificationInterneRepository;
import com.example.demo.repository.UtilisateurRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationInterneService {
    private final NotificationInterneRepository notifications;
    private final UtilisateurRepository utilisateurs;
    private final EquipementRepository equipements;

    public NotificationInterneService(NotificationInterneRepository notifications,
            UtilisateurRepository utilisateurs, EquipementRepository equipements) {
        this.notifications = notifications;
        this.utilisateurs = utilisateurs;
        this.equipements = equipements;
    }

    public NotificationInterne creer(Utilisateur destinataire, String titre, String message,
            String type, Long referenceId, StatutNotification statut) {
        if (destinataire == null) return null;
        NotificationInterne notification = new NotificationInterne();
        notification.setDestinataire(destinataire);
        notification.setTitre(titre);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReferenceId(referenceId);
        notification.setStatut(statut);
        return notifications.save(notification);
    }

    public void notifierAdmins(String titre, String message, String type, Long referenceId) {
        utilisateurs.findByRoleInAndActifTrue(List.of(RoleType.ADMIN))
                .forEach(admin -> creer(admin, titre, message, type, referenceId, StatutNotification.NOUVELLE));
    }

    public void notifierTechniciens(String titre, String message, String type, Long referenceId) {
        utilisateurs.findByRoleInAndActifTrue(List.of(RoleType.TECHNICIEN))
                .forEach(technicien -> creer(technicien, titre, message, type, referenceId, StatutNotification.NOUVELLE));
    }

    public void notifierUtilisateur(Utilisateur utilisateur, String titre, String message,
            String type, Long referenceId) {
        if (utilisateur != null && utilisateur.isActif()) {
            creer(utilisateur, titre, message, type, referenceId, StatutNotification.NOUVELLE);
        }
    }

    @Transactional
    public void affecterTache(String type, Long referenceId, Utilisateur technicien) {
        List<NotificationInterne> groupe = notifications.findGroupForUpdate(type, referenceId);
        boolean trouvee = false;
        for (NotificationInterne notification : groupe) {
            boolean selectionnee = notification.getDestinataire().getId().equals(technicien.getId());
            notification.setStatut(selectionnee ? StatutNotification.EN_COURS : StatutNotification.AFFECTEE);
            notification.setLu(!selectionnee);
            notifications.save(notification);
            trouvee = trouvee || selectionnee;
        }
        if (!trouvee) {
            creer(
                    technicien,
                    "Tache affectee",
                    "Vous avez pris en charge la tache #" + referenceId,
                    type,
                    referenceId,
                    StatutNotification.EN_COURS);
        }
    }

    public List<NotificationInterne> pourUtilisateur(String login) {
        return notifications.findByDestinataireLoginOrderByDateCreationDesc(login);
    }

    @Transactional
    public void terminer(String type, Long referenceId) {
        notifications.findByTypeAndReferenceId(type, referenceId).forEach(notification -> {
            notification.setStatut(StatutNotification.DONE);
            notification.setLu(true);
            notifications.save(notification);
        });
    }

    public NotificationInterne marquerLue(Long id, String login) {
        NotificationInterne notification = notifications.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification introuvable: " + id));
        if (!notification.getDestinataire().getLogin().equals(login)) {
            throw new IllegalArgumentException("Notification non autorisee");
        }
        notification.setLu(true);
        return notifications.save(notification);
    }

    @Transactional
    public NotificationInterne affecterEquipement(Long id, String login) {
        NotificationInterne notification = notificationAutorisee(id, login);
        if (!"EQUIPEMENT_AFFECTE".equals(notification.getType())) {
            throw new IllegalArgumentException("Cette notification n'est pas une tache equipement");
        }
        List<NotificationInterne> groupe = notifications.findGroupForUpdate(
                notification.getType(), notification.getReferenceId());
        if (groupe.stream().anyMatch(item -> item.getStatut() == StatutNotification.EN_COURS)) {
            throw new IllegalArgumentException("Cette tache est deja affectee a un autre technicien");
        }
        groupe.forEach(item -> {
            boolean choisie = item.getId().equals(id);
            item.setStatut(choisie ? StatutNotification.EN_COURS : StatutNotification.AFFECTEE);
            notifications.save(item);
        });
        return notifications.findById(id).orElseThrow();
    }

    @Transactional
    public NotificationInterne terminerEquipement(Long id, String login) {
        NotificationInterne notification = notificationAutorisee(id, login);
        if (!"EQUIPEMENT_AFFECTE".equals(notification.getType())
                || notification.getStatut() != StatutNotification.EN_COURS) {
            throw new IllegalArgumentException("Cette tache n'est pas en cours");
        }
        var equipement = equipements.findById(notification.getReferenceId())
                .orElseThrow(() -> new NotFoundException(
                        "Equipement introuvable: " + notification.getReferenceId()));
        if (equipement.getStatut() != StatutEquipement.EN_ATTENTE_AFFECTATION) {
            throw new IllegalArgumentException("Cet equipement n'est plus en attente d'affectation");
        }
        equipement.setStatut(StatutEquipement.AFFECTE);
        equipements.save(equipement);
        notifications.findGroupForUpdate(notification.getType(), notification.getReferenceId())
                .forEach(item -> {
                    item.setStatut(StatutNotification.DONE);
                    item.setLu(true);
                    notifications.save(item);
                });
        notificationInterneAdmin(
                "Affectation equipement terminee",
                "L'equipement #" + notification.getReferenceId() + " a ete termine par " + login,
                "EQUIPEMENT_DONE",
                notification.getReferenceId());
        return notifications.findById(id).orElseThrow();
    }

    private NotificationInterne notificationAutorisee(Long id, String login) {
        NotificationInterne notification = notifications.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification introuvable: " + id));
        if (!notification.getDestinataire().getLogin().equals(login)
                || notification.getDestinataire().getRole() != RoleType.TECHNICIEN) {
            throw new IllegalArgumentException("Notification non autorisee");
        }
        return notification;
    }

    private void notificationInterneAdmin(String titre, String message, String type, Long referenceId) {
        notifierAdmins(titre, message, type, referenceId);
    }
}
