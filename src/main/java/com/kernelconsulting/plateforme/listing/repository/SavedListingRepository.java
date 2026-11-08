package com.kernelconsulting.plateforme.listing.repository;

import com.kernelconsulting.plateforme.listing.domain.SavedListing;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SavedListingRepository extends JpaRepository<SavedListing, UUID> {

    @Query("select s from SavedListing s join fetch s.listing l join fetch l.organization where s.user.id = :userId order by s.createdAt desc")
    List<SavedListing> findWithListing(UUID userId);

    boolean existsByUserIdAndListingId(UUID userId, UUID listingId);

    void deleteByUserIdAndListingId(UUID userId, UUID listingId);
}
