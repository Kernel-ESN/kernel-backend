package com.kernelconsulting.plateforme.moderation.services;

import com.kernelconsulting.plateforme.account.domain.AppUser;
import com.kernelconsulting.plateforme.account.domain.Organization;
import com.kernelconsulting.plateforme.account.domain.Verification;
import com.kernelconsulting.plateforme.account.repository.AppUserRepository;
import com.kernelconsulting.plateforme.account.repository.OrganizationRepository;
import com.kernelconsulting.plateforme.common.exception.ApiException;
import com.kernelconsulting.plateforme.common.security.CurrentUser;
import com.kernelconsulting.plateforme.common.storage.FileStorage;
import com.kernelconsulting.plateforme.listing.domain.Listing;
import com.kernelconsulting.plateforme.listing.domain.ListingStatus;
import com.kernelconsulting.plateforme.listing.repository.ListingRepository;
import com.kernelconsulting.plateforme.moderation.api.dto.ModerationListingResponse;
import com.kernelconsulting.plateforme.moderation.api.dto.ModerationOrganizationResponse;
import com.kernelconsulting.plateforme.moderation.domain.ModerationAction;
import com.kernelconsulting.plateforme.moderation.domain.ModerationLog;
import com.kernelconsulting.plateforme.moderation.repository.ModerationLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Décisions des modérateurs. Toute annonce passe par une validation (ADR 0007), chaque décision est
 * tracée, et une décision négative porte toujours un motif communiqué à l'organisation.
 */
@Service
@RequiredArgsConstructor
public class ModerationService {

    private static final int MAX_ROWS = 200;
    private static final String LISTING = "ANNONCE";
    private static final String ORGANIZATION = "ORGANISATION";

    private final ListingRepository listings;
    private final OrganizationRepository organizations;
    private final AppUserRepository users;
    private final ModerationLogRepository log;
    private final FileStorage storage;
    private final CurrentUser currentUser;
    private final Clock clock;

    /** File d'attente, les plus anciennes d'abord ; ou annonces publiées, les plus récentes d'abord. */
    @Transactional(readOnly = true)
    public List<ModerationListingResponse> listings(ListingStatus status) {
        Sort sort = status == ListingStatus.EN_ATTENTE
                ? Sort.by("updatedAt").ascending()
                : Sort.by("updatedAt").descending();
        return listings.findByStatus(status, PageRequest.of(0, MAX_ROWS, sort)).stream()
                .map(ModerationService::toResponse)
                .toList();
    }

    /**
     * Publie une annonce en attente.
     *
     * @throws ApiException 409 si elle n'est plus en attente, 422 si sa date limite est passée
     */
    @Transactional
    public ModerationListingResponse publish(UUID id) {
        Listing listing = pending(id);
        Instant now = clock.instant();
        if (listing.isExpiredAt(now)) {
            throw ApiException.unprocessable("La date limite de cette annonce est passée.");
        }
        listing.setStatus(ListingStatus.PUBLIEE);
        listing.setPublishedAt(now);
        listing.setModerationNote(null);
        listing.setUpdatedAt(now);
        trace(LISTING, id, ModerationAction.PUBLIER, null);
        return toResponse(listing);
    }

    /** Renvoie une annonce en brouillon, avec le motif à corriger. */
    @Transactional
    public ModerationListingResponse reject(UUID id, String reason) {
        Listing listing = pending(id);
        listing.setStatus(ListingStatus.BROUILLON);
        listing.setModerationNote(reason.trim());
        listing.setUpdatedAt(clock.instant());
        trace(LISTING, id, ModerationAction.REFUSER, reason.trim());
        return toResponse(listing);
    }

    /**
     * Retire une annonce publiée, avec le motif communiqué à l'organisation.
     *
     * @throws ApiException 409 si elle n'est pas publiée
     */
    @Transactional
    public ModerationListingResponse withdraw(UUID id, String reason) {
        Listing listing = find(id);
        if (listing.getStatus() != ListingStatus.PUBLIEE) {
            throw ApiException.conflict("Seule une annonce publiée peut être retirée.");
        }
        listing.setStatus(ListingStatus.RETIREE);
        listing.setModerationNote(reason.trim());
        listing.setUpdatedAt(clock.instant());
        trace(LISTING, id, ModerationAction.RETIRER, reason.trim());
        return toResponse(listing);
    }

    /** Organisations à vérifier, les plus anciennes d'abord. */
    @Transactional(readOnly = true)
    public List<ModerationOrganizationResponse> organizations(Verification verification) {
        return organizations.findByVerificationOrderByCreatedAtAsc(verification).stream().map(this::toResponse).toList();
    }

    /** Accorde le badge « Vérifié », qui apparaît aussitôt sur les annonces de l'organisation. */
    @Transactional
    public ModerationOrganizationResponse verify(UUID id) {
        Organization organization = pendingOrganization(id);
        organization.setVerification(Verification.VERIFIEE);
        organization.setVerificationNote(null);
        trace(ORGANIZATION, id, ModerationAction.VERIFIER, null);
        return toResponse(organization);
    }

    /** Refuse le justificatif : l'organisation peut en envoyer un autre. */
    @Transactional
    public ModerationOrganizationResponse rejectOrganization(UUID id, String reason) {
        Organization organization = pendingOrganization(id);
        organization.setVerification(Verification.REFUSEE);
        organization.setVerificationNote(reason.trim());
        trace(ORGANIZATION, id, ModerationAction.REFUSER_ORGANISATION, reason.trim());
        return toResponse(organization);
    }

    /** Fichier justificatif et nom à proposer au téléchargement. */
    public record Document(Resource content, String name) {
    }

    /** @throws ApiException 404 si l'organisation ou son fichier n'existe pas */
    @Transactional(readOnly = true)
    public Document document(UUID id) {
        Organization organization = organizations.findById(id)
                .orElseThrow(() -> ApiException.notFound("Cette organisation n'existe pas."));
        Resource content = storage.read(organization.getDocumentPath())
                .orElseThrow(() -> ApiException.notFound("Cette organisation n'a pas de justificatif."));
        return new Document(content, organization.getDocumentName());
    }

    private Listing pending(UUID id) {
        Listing listing = find(id);
        if (listing.getStatus() != ListingStatus.EN_ATTENTE) {
            throw ApiException.conflict("Cette annonce n'est plus en attente.");
        }
        return listing;
    }

    private Listing find(UUID id) {
        return listings.findById(id).orElseThrow(() -> ApiException.notFound("Cette annonce n'existe pas."));
    }

    private Organization pendingOrganization(UUID id) {
        Organization organization = organizations.findById(id)
                .orElseThrow(() -> ApiException.notFound("Cette organisation n'existe pas."));
        if (organization.getVerification() != Verification.EN_ATTENTE) {
            throw ApiException.conflict("Cette organisation n'est plus en attente.");
        }
        return organization;
    }

    private void trace(String type, UUID id, ModerationAction action, String note) {
        log.save(new ModerationLog(currentUser.id(), type, id, action, note, clock.instant()));
    }

    private ModerationOrganizationResponse toResponse(Organization organization) {
        String email = users.findFirstByOrganizationId(organization.getId()).map(AppUser::getEmail).orElse(null);
        return new ModerationOrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getType().name(),
                organization.getVerification().name(),
                organization.getContactPhone(),
                email,
                organization.getDocumentName(),
                organization.getVerificationNote(),
                organization.getCreatedAt());
    }

    private static ModerationListingResponse toResponse(Listing listing) {
        Organization organization = listing.getOrganization();
        return new ModerationListingResponse(
                listing.getId(),
                listing.getSlug(),
                listing.getUniverse().name(),
                listing.getType().name(),
                listing.getTitle(),
                listing.getSummary(),
                listing.getDescription(),
                new ModerationListingResponse.OrganizationPart(
                        organization.getId(), organization.getName(), organization.getVerification().name()),
                listing.getOwner() == null ? null : listing.getOwner().getEmail(),
                listing.getSectorId(),
                listing.getRegionId(),
                listing.getCity(),
                listing.getDeadline(),
                listing.getApplyMode().name(),
                listing.getApplyEmail(),
                listing.getApplyUrl(),
                listing.getApplyInstructions(),
                listing.getBudgetAmount(),
                listing.getStatus().name(),
                listing.getUpdatedAt());
    }
}
