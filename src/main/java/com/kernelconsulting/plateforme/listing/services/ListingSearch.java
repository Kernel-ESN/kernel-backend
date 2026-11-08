package com.kernelconsulting.plateforme.listing.services;

import com.kernelconsulting.plateforme.account.domain.OrganizationType;
import com.kernelconsulting.plateforme.listing.domain.ListingSort;
import com.kernelconsulting.plateforme.listing.domain.ListingType;
import com.kernelconsulting.plateforme.listing.domain.Universe;
import java.util.List;
import java.util.UUID;

/** Critères d'une recherche, déjà contrôlés. Une liste vide ne filtre pas. */
public record ListingSearch(
        Universe universe,
        List<ListingType> types,
        List<String> sectorIds,
        List<String> regionIds,
        List<String> educationLevels,
        List<OrganizationType> organizationTypes,
        UUID organizationId,
        Integer deadlineWithinDays,
        Integer publishedWithinDays,
        String text,
        ListingSort sort,
        int offset,
        int limit) {
}
