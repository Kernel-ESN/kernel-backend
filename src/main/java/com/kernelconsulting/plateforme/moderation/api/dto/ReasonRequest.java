package com.kernelconsulting.plateforme.moderation.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motif communiqué à l'organisation : une décision négative s'explique toujours. */
public record ReasonRequest(@NotBlank @Size(min = 5, max = 500) String reason) {
}
