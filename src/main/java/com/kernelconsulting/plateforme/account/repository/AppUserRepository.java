package com.kernelconsulting.plateforme.account.repository;

import com.kernelconsulting.plateforme.account.domain.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<AppUser> findFirstByOrganizationId(UUID organizationId);
}
