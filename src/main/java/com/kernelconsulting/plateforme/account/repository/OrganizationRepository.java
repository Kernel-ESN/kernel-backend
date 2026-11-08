package com.kernelconsulting.plateforme.account.repository;

import com.kernelconsulting.plateforme.account.domain.Organization;
import com.kernelconsulting.plateforme.account.domain.Verification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findFirstByName(String name);

    List<Organization> findByVerificationOrderByCreatedAtAsc(Verification verification);
}
