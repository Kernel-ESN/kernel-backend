package com.kernelconsulting.plateforme.listing.api.dto;

import java.util.Map;

/** Nombre d'annonces par valeur de filtre, chaque filtre ignorant ses propres choix. */
public record ListingFacetsResponse(
        Map<String, Long> type,
        Map<String, Long> sector,
        Map<String, Long> region,
        Map<String, Long> education,
        Map<String, Long> organizationType,
        Map<String, Long> within,
        Map<String, Long> since) {
}
