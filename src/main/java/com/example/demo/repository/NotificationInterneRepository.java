package com.example.demo.repository;

import com.example.demo.domain.NotificationInterne;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationInterneRepository extends JpaRepository<NotificationInterne, Long> {
    List<NotificationInterne> findByDestinataireLoginOrderByDateCreationDesc(String login);
    List<NotificationInterne> findByTypeAndReferenceId(String type, Long referenceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NotificationInterne n where n.type = :type and n.referenceId = :referenceId")
    List<NotificationInterne> findGroupForUpdate(
            @Param("type") String type, @Param("referenceId") Long referenceId);
}
