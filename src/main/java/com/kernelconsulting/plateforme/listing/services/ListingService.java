package com.kernelconsulting.plateforme.listing.services;

import com.kernelconsulting.plateforme.common.exception.ApiException;
import com.kernelconsulting.plateforme.listing.api.dto.ListingFacetsResponse;
import com.kernelconsulting.plateforme.listing.api.dto.ListingPageResponse;
import com.kernelconsulting.plateforme.listing.api.dto.ListingResponse;
import com.kernelconsulting.plateforme.listing.api.dto.PublicOrganizationResponse;
import com.kernelconsulting.plateforme.listing.api.mapper.ListingMapper;
import com.kernelconsulting.plateforme.listing.domain.Listing;
import com.kernelconsulting.plateforme.listing.domain.ListingStatus;
import com.kernelconsulting.plateforme.listing.domain.Universe;
import com.kernelconsulting.plateforme.listing.repository.ListingRepository;
import com.kernelconsulting.plateforme.listing.repository.ListingSearchRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lecture publique des annonces. */
@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listings;
    private final ListingSearchRepository searchRepository;
    private final Clock clock;

    /** Recherche parmi les annonces publiées et non expirées, dans l'ordre demandé. */
    @Transactional(readOnly = true)
    public ListingPageResponse search(ListingSearch search) {
        Instant now = clock.instant();
        ListingSearchRepository.Result result = searchRepository.search(search, now);
        Map<UUID, Listing> byId = listings.findByIdIn(result.ids()).stream()
                .collect(Collectors.toMap(Listing::getId, Function.identity()));
        List<ListingResponse> items = result.ids().stream()
                .map(byId::get)
                .map(listing -> ListingMapper.toResponse(listing, now))
                .toList();
        return new ListingPageResponse(items, result.total());
    }

    /** Compteurs affichés à côté de chaque filtre pour la recherche en cours. */
    @Transactional(readOnly = true)
    public ListingFacetsResponse facets(ListingSearch search) {
        return searchRepository.facets(search, clock.instant());
    }

    /** Organisations qui ont des annonces ouvertes. */
    @Transactional(readOnly = true)
    public List<PublicOrganizationResponse> organizations() {
        return searchRepository.organizations(clock.instant());
    }

    /**
     * Lit la page publique d'une organisation.
     *
     * @throws ApiException 404 si elle n'a jamais eu d'annonce publiée
     */
    @Transactional(readOnly = true)
    public PublicOrganizationResponse organization(String slug) {
        return searchRepository.organization(slug, clock.instant())
                .orElseThrow(() -> ApiException.notFound("Cette organisation n'existe pas."));
    }

    /**
     * Lit une annonce publiée, même expirée : sa page reste utile au référencement.
     *
     * @throws ApiException 404 si elle est inconnue, retirée ou pas encore publiée
     */
    @Transactional
    public ListingResponse getPublic(Universe universe, String slug) {
        Listing listing = listings.findByUniverseAndSlug(universe, slug)
                .filter(found -> found.getStatus() == ListingStatus.PUBLIEE)
                .orElseThrow(() -> ApiException.notFound("Cette annonce n'existe pas ou a été retirée."));
        listings.incrementViews(listing.getId());
        return ListingMapper.toResponse(listing, clock.instant());
    }

    /** Annonces actives du même univers, d'abord du même secteur puis de la même région. */
    @Transactional(readOnly = true)
    public ListingPageResponse similar(UUID id, int limit) {
        Listing source = listings.findById(id)
                .orElseThrow(() -> ApiException.notFound("Cette annonce n'existe pas ou a été retirée."));
        Instant now = clock.instant();
        List<ListingResponse> items = listings.findSimilar(source, ListingStatus.PUBLIEE, now, PageRequest.of(0, limit)).stream()
                .map(listing -> ListingMapper.toResponse(listing, now))
                .toList();
        return new ListingPageResponse(items, items.size());
    }

    /** Nombre d'annonces actives par univers, zéro compris. */
    @Transactional(readOnly = true)
    public Map<Universe, Long> counts() {
        Map<Universe, Long> counts = new EnumMap<>(Universe.class);
        for (Universe universe : Universe.values()) {
            counts.put(universe, 0L);
        }
        for (Object[] row : listings.countActiveByUniverse(ListingStatus.PUBLIEE, clock.instant())) {
            counts.put((Universe) row[0], (Long) row[1]);
        }
        return counts;
    }
}
