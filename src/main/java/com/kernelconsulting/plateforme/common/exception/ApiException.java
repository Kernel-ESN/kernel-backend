package com.kernelconsulting.plateforme.common.exception;

import java.util.List;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Erreur métier : le message, en français, est destiné à l'écran. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final List<String> details;

    public ApiException(HttpStatus status, String message) {
        this(status, message, List.of());
    }

    public ApiException(HttpStatus status, String message, List<String> details) {
        super(message);
        this.status = status;
        this.details = details;
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    public static ApiException unprocessable(String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public static ApiException badRequest(String message, List<String> details) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, details);
    }
}
