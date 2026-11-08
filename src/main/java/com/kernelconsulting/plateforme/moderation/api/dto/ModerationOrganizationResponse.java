package com.kernelconsulting.plateforme.moderation.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ModerationOrganizationResponse(
        UUID id,
        String name,
        String type,
        String verification,
        String contactPhone,
        String contactEmail,
        String documentName,
        String verificationNote,
        Instant createdAt) {
}
