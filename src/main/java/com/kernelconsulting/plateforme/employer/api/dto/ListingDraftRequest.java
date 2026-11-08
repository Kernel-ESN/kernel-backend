package com.kernelconsulting.plateforme.employer.api.dto;

import com.kernelconsulting.plateforme.listing.domain.ApplyMode;
import com.kernelconsulting.plateforme.listing.domain.ListingType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Un brouillon peut être incomplet : les règles de fond s'appliquent à la soumission. */
public record ListingDraftRequest(
        @NotNull ListingType type,
        @Size(max = 140) String title,
        @Size(max = 300) String summary,
        @Size(max = 10000) String description,
        @Size(max = 40) String sectorId,
        @Size(max = 40) String regionId,
        @Size(max = 80) String city,
        String deadline,
        @NotNull ApplyMode applyMode,
        @Size(max = 254) String applyEmail,
        @Size(max = 500) String applyUrl,
        @Size(max = 1000) String instructions,
        @Positive BigDecimal budget) {
}
