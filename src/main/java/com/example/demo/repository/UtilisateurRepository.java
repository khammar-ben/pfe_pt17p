package com.example.demo.repository;

import com.example.demo.domain.Utilisateur;
import com.example.demo.domain.RoleType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByLogin(String login);

    boolean existsByEmployeId(Long employeId);

    List<Utilisateur> findByRoleInAndActifTrue(Collection<RoleType> roles);
}
