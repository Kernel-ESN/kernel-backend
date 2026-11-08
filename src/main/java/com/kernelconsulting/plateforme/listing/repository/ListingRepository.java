package com.kernelconsulting.plateforme.listing.repository;

import com.kernelconsulting.plateforme.listing.domain.Listing;
import com.kernelconsulting.plateforme.listing.domain.ListingStatus;
import com.kernelconsulting.plateforme.listing.domain.Universe;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ListingRepository extends JpaRepository<Listing, UUID> {

    @EntityGraph(attributePaths = "organization")
    Optional<Listing> findByUniverseAndSlug(Universe universe, String slug);

    Optional<Listing> findBySlug(String slug);

    @EntityGraph(attributePaths = {"organization", "owner"})
    List<Listing> findByStatus(ListingStatus status, Pageable page);

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = "organization")
    List<Listing> findByIdIn(Collection<UUID> ids);

    @Query("""
            select l from Listing l join fetch l.organization
            where l.id <> :#{#source.id} and l.universe = :#{#source.universe}
              and l.status = :status and l.deadline >= :now
              and (l.sectorId = :#{#source.sectorId} or l.regionId = :#{#source.regionId})
            order by case when l.sectorId = :#{#source.sectorId} then 2 else 0 end
                   + case when l.regionId = :#{#source.regionId} then 1 else 0 end desc,
                     l.publishedAt desc
            """)
    List<Listing> findSimilar(Listing source, ListingStatus status, Instant now, Pageable page);

    @Query("select l.universe, count(l) from Listing l where l.status = :status and l.deadline >= :now group by l.universe")
    List<Object[]> countActiveByUniverse(ListingStatus status, Instant now);

    @Modifying
    @Query("update Listing l set l.viewCount = l.viewCount + 1 where l.id = :id")
    void incrementViews(UUID id);

    @EntityGraph(attributePaths = "organization")
    List<Listing> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    @EntityGraph(attributePaths = "organization")
    Optional<Listing> findByIdAndOwnerId(UUID id, UUID ownerId);
}
