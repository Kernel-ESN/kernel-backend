package com.kernelconsulting.plateforme.moderation.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ModerationListingResponse(
        UUID id,
        String slug,
        String universe,
        String type,
        String title,
        String summary,
        String description,
        OrganizationPart organization,
        String submittedBy,
        String sectorId,
        String regionId,
        String city,
        Instant deadline,
        String applyMode,
        String applyEmail,
        String applyUrl,
        String instructions,
        BigDecimal budget,
        String status,
        Instant updatedAt) {

    public record OrganizationPart(UUID id, String name, String verification) {
    }
}
