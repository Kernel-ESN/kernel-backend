package com.kernelconsulting.plateforme.alert.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        UUID id, AlertCriteria criteria, String email, String channel, String frequency, Instant createdAt) {
}
