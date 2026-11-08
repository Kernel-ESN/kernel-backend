package com.kernelconsulting.plateforme.moderation.api.controller;

import com.kernelconsulting.plateforme.account.domain.Verification;
import com.kernelconsulting.plateforme.listing.domain.ListingStatus;
import com.kernelconsulting.plateforme.moderation.api.dto.ModerationListingResponse;
import com.kernelconsulting.plateforme.moderation.api.dto.ModerationOrganizationResponse;
import com.kernelconsulting.plateforme.moderation.api.dto.ReasonRequest;
import com.kernelconsulting.plateforme.moderation.services.ModerationService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Réservé au rôle ADMIN par {@code SecurityConfig}. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService service;

    @GetMapping("/listings")
    List<ModerationListingResponse> listings(@RequestParam(defaultValue = "EN_ATTENTE") ListingStatus status) {
        return service.listings(status);
    }

    @PostMapping("/listings/{id}/publish")
    ModerationListingResponse publish(@PathVariable UUID id) {
        return service.publish(id);
    }

    @PostMapping("/listings/{id}/reject")
    ModerationListingResponse reject(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request) {
        return service.reject(id, request.reason());
    }

    @PostMapping("/listings/{id}/withdraw")
    ModerationListingResponse withdraw(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request) {
        return service.withdraw(id, request.reason());
    }

    @GetMapping("/organizations")
    List<ModerationOrganizationResponse> organizations(@RequestParam(defaultValue = "EN_ATTENTE") Verification verification) {
        return service.organizations(verification);
    }

    @PostMapping("/organizations/{id}/verify")
    ModerationOrganizationResponse verify(@PathVariable UUID id) {
        return service.verify(id);
    }

    @PostMapping("/organizations/{id}/reject")
    ModerationOrganizationResponse rejectOrganization(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request) {
        return service.rejectOrganization(id, request.reason());
    }

    /** Toujours en téléchargement : un fichier déposé par un tiers ne s'ouvre jamais dans le site. */
    @GetMapping("/organizations/{id}/document")
    ResponseEntity<Resource> document(@PathVariable UUID id) {
        ModerationService.Document document = service.document(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(document.name() == null ? "justificatif" : document.name(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(document.content());
    }
}
