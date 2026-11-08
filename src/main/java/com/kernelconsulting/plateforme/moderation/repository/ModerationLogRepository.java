package com.kernelconsulting.plateforme.moderation.repository;

import com.kernelconsulting.plateforme.moderation.domain.ModerationLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationLogRepository extends JpaRepository<ModerationLog, UUID> {

    List<ModerationLog> findByTargetIdOrderByCreatedAtAsc(UUID targetId);
}
