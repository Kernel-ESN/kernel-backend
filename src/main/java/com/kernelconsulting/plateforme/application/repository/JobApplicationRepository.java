package com.kernelconsulting.plateforme.application.repository;

import com.kernelconsulting.plateforme.application.domain.JobApplication;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    @Query("select a from JobApplication a join fetch a.listing l join fetch l.organization where a.user.id = :userId order by a.createdAt desc")
    List<JobApplication> findMine(UUID userId);

    boolean existsByUserIdAndListingId(UUID userId, UUID listingId);

    long countByListingId(UUID listingId);
}
