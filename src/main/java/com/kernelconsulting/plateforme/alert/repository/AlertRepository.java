package com.kernelconsulting.plateforme.alert.repository;

import com.kernelconsulting.plateforme.alert.domain.Alert;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    @EntityGraph(attributePaths = "user")
    List<Alert> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Alert> findByIdAndUserId(UUID id, UUID userId);
}
