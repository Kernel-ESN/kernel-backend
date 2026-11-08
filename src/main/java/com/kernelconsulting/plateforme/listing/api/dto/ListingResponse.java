package com.kernelconsulting.plateforme.listing.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ListingResponse(
        UUID id,
        String slug,
        String universe,
        String type,
        String reference,
        String title,
        String summary,
        List<String> description,
        OrganizationPart organization,
        String sectorId,
        String regionId,
        String city,
        String country,
        String educationLevel,
        String experienceLevel,
        Instant publishedAt,
        Instant deadline,
        String status,
        HowToApply howToApply,
        Budget budget,
        List<Attachment> attachments,
        @JsonProperty("isSample") boolean sample) {

    public record OrganizationPart(UUID id, String slug, String name, String type, boolean verified) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HowToApply(String mode, String email, String url, String instructions) {
    }

    public record Budget(BigDecimal amount, String currency, boolean hidden) {
    }

    public record Attachment(String name, String url, long sizeBytes) {
    }
}
