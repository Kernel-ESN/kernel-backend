package com.kernelconsulting.plateforme.account.api.dto;

import java.time.Instant;

public record CvResponse(String name, long sizeBytes, Instant updatedAt) {
}
