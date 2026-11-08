package com.kernelconsulting.plateforme.common.exception;

import java.time.Instant;
import java.util.List;

/** Corps unique de toutes les erreurs de l'API. */
public record ErrorResponse(
        Instant timestamp, int status, String error, String message, String path, List<String> details) {
}
