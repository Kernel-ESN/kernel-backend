package com.kernelconsulting.plateforme.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/** Traduit toute erreur en {@link ErrorResponse}. Aucun détail technique ne sort de l'API. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> api(ApiException e, HttpServletRequest request) {
        return body(e.getStatus(), e.getMessage(), request, e.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> invalid(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " : " + error.getDefaultMessage())
                .toList();
        return body(HttpStatus.BAD_REQUEST, "Certains champs ne sont pas valides.", request, details);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class,
        MissingServletRequestPartException.class
    })
    ResponseEntity<ErrorResponse> malformed(Exception e, HttpServletRequest request) {
        return body(HttpStatus.BAD_REQUEST, "La requête est incomplète ou mal formée.", request, List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> tooLarge(MaxUploadSizeExceededException e, HttpServletRequest request) {
        return body(HttpStatus.UNPROCESSABLE_ENTITY, "Ce fichier dépasse 5 Mo.", request, List.of());
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ErrorResponse> credentials(BadCredentialsException e, HttpServletRequest request) {
        return body(HttpStatus.UNAUTHORIZED, "E-mail ou mot de passe incorrect.", request, List.of());
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    ResponseEntity<ErrorResponse> anonymous(Exception e, HttpServletRequest request) {
        return body(HttpStatus.UNAUTHORIZED, "Connectez-vous pour continuer.", request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> denied(AccessDeniedException e, HttpServletRequest request) {
        return body(HttpStatus.FORBIDDEN, "Vous n'avez pas accès à cette ressource.", request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("Violation d'intégrité sur {}", request.getRequestURI(), e);
        return body(HttpStatus.CONFLICT, "Cette opération entre en conflit avec des données existantes.", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest request) {
        log.error("Erreur inattendue sur {}", request.getRequestURI(), e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur est survenue. Réessayez.", request, List.of());
    }

    private ResponseEntity<ErrorResponse> body(
            HttpStatus status, String message, HttpServletRequest request, List<String> details) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI(), details);
        return ResponseEntity.status(status).body(response);
    }
}
