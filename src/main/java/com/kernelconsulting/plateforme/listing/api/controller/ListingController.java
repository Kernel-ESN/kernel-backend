package com.kernelconsulting.plateforme.listing.api.controller;

import com.kernelconsulting.plateforme.account.domain.OrganizationType;
import com.kernelconsulting.plateforme.common.exception.ApiException;
import com.kernelconsulting.plateforme.listing.api.dto.ListingFacetsResponse;
import com.kernelconsulting.plateforme.listing.api.dto.ListingPageResponse;
import com.kernelconsulting.plateforme.listing.api.dto.ListingResponse;
import com.kernelconsulting.plateforme.listing.domain.ListingSort;
import com.kernelconsulting.plateforme.listing.domain.ListingType;
import com.kernelconsulting.plateforme.listing.domain.Universe;
import com.kernelconsulting.plateforme.listing.services.ListingSearch;
import com.kernelconsulting.plateforme.listing.services.ListingService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private static final int MAX_LIMIT = 600;
    private static final int MAX_VALUES = 20;

    private final ListingService service;

    @GetMapping
    ListingPageResponse search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Universe universe,
            @RequestParam(required = false) List<ListingType> type,
            @RequestParam(required = false) List<String> sectorId,
            @RequestParam(required = false) List<String> regionId,
            @RequestParam(required = false) List<String> educationLevel,
            @RequestParam(required = false) List<OrganizationType> organizationType,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) Integer deadlineWithinDays,
            @RequestParam(required = false) Integer publishedWithinDays,
            @RequestParam(defaultValue = "recent") ListingSort sort,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "12") int limit) {
        if (offset < 0 || limit < 1 || limit > MAX_LIMIT) {
            throw invalid();
        }
        return service.search(criteria(
                q, universe, type, sectorId, regionId, educationLevel, organizationType, organizationId,
                deadlineWithinDays, publishedWithinDays, sort, offset, limit));
    }

    @GetMapping("/facets")
    ListingFacetsResponse facets(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Universe universe,
            @RequestParam(required = false) List<ListingType> type,
            @RequestParam(required = false) List<String> sectorId,
            @RequestParam(required = false) List<String> regionId,
            @RequestParam(required = false) List<String> educationLevel,
            @RequestParam(required = false) List<OrganizationType> organizationType,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) Integer deadlineWithinDays,
            @RequestParam(required = false) Integer publishedWithinDays) {
        return service.facets(criteria(
                q, universe, type, sectorId, regionId, educationLevel, organizationType, organizationId,
                deadlineWithinDays, publishedWithinDays, ListingSort.recent, 0, 1));
    }

    private static ListingSearch criteria(
            String q,
            Universe universe,
            List<ListingType> types,
            List<String> sectors,
            List<String> regions,
            List<String> educations,
            List<OrganizationType> organizationTypes,
            UUID organizationId,
            Integer deadlineWithinDays,
            Integer publishedWithinDays,
            ListingSort sort,
            int offset,
            int limit) {
        if ((deadlineWithinDays != null && deadlineWithinDays != 7 && deadlineWithinDays != 30)
                || (publishedWithinDays != null && publishedWithinDays != 1 && publishedWithinDays != 7
                        && publishedWithinDays != 30)
                || (q != null && q.length() > 120)
                || tooMany(types) || tooMany(sectors) || tooMany(regions) || tooMany(educations)
                || tooMany(organizationTypes)) {
            throw invalid();
        }
        String text = q == null || q.isBlank() ? null : q.trim();
        return new ListingSearch(
                universe, orEmpty(types), orEmpty(sectors), orEmpty(regions), orEmpty(educations),
                orEmpty(organizationTypes), organizationId, deadlineWithinDays, publishedWithinDays, text, sort, offset,
                limit);
    }

    private static boolean tooMany(List<?> values) {
        return values != null && values.size() > MAX_VALUES;
    }

    private static <T> List<T> orEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static ApiException invalid() {
        return ApiException.badRequest("Les critères de recherche ne sont pas valides.", List.of());
    }

    @GetMapping("/counts")
    Map<Universe, Long> counts() {
        return service.counts();
    }

    @GetMapping("/{universe}/{slug}")
    ListingResponse get(@PathVariable Universe universe, @PathVariable String slug) {
        return service.getPublic(universe, slug);
    }

    @GetMapping("/{id}/similar")
    ListingPageResponse similar(@PathVariable UUID id, @RequestParam(defaultValue = "3") int limit) {
        return service.similar(id, Math.clamp(limit, 1, 12));
    }
}
