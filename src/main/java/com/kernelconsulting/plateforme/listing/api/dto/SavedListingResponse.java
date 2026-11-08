package com.kernelconsulting.plateforme.listing.api.dto;

import java.util.UUID;

public record SavedListingResponse(UUID id, String slug, String universe, String title, String organizationName) {
}
