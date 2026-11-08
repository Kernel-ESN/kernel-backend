package com.kernelconsulting.plateforme.listing.api.dto;

import java.util.UUID;

/** Ce qu'un visiteur voit d'une organisation : jamais ses contacts ni son justificatif. */
public record PublicOrganizationResponse(
        UUID id, String slug, String name, String type, boolean verified, long openListings) {
}
