package com.kernelconsulting.plateforme.employer.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EmployerListingResponse(
        UUID id,
        UUID ownerId,
        String type,
        String title,
        String summary,
        String description,
        String sectorId,
        String regionId,
        String city,
        String deadline,
        String applyMode,
        String applyEmail,
        String applyUrl,
        String instructions,
        BigDecimal budget,
        String status,
        String moderationNote,
        long views,
        long applications,
        Instant updatedAt) {
}
