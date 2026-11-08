package com.kernelconsulting.plateforme.application.api.dto;

import com.kernelconsulting.plateforme.listing.api.dto.SavedListingResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApplicationResponse(
        UUID id, SavedListingResponse listing, UUID userId, String status, Instant createdAt, List<String> documents) {
}
