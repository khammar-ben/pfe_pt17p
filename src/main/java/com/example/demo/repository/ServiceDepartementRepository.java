package com.example.demo.repository;

import com.example.demo.domain.ServiceDepartement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceDepartementRepository extends JpaRepository<ServiceDepartement, Long> {
}
