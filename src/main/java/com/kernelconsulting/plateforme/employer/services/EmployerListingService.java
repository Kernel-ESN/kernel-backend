package com.kernelconsulting.plateforme.employer.services;

import com.kernelconsulting.plateforme.account.domain.AppUser;
import com.kernelconsulting.plateforme.account.repository.AppUserRepository;
import com.kernelconsulting.plateforme.application.repository.JobApplicationRepository;
import com.kernelconsulting.plateforme.common.exception.ApiException;
import com.kernelconsulting.plateforme.common.security.CurrentUser;
import com.kernelconsulting.plateforme.employer.api.dto.EmployerListingResponse;
import com.kernelconsulting.plateforme.employer.api.dto.ListingDraftRequest;
import com.kernelconsulting.plateforme.listing.api.mapper.ListingMapper;
import com.kernelconsulting.plateforme.listing.domain.Listing;
import com.kernelconsulting.plateforme.listing.domain.ListingStatus;
import com.kernelconsulting.plateforme.listing.repository.ListingRepository;
import com.kernelconsulting.plateforme.listing.services.Slugs;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Annonces d'une organisation. Une soumission attend la validation : rien n'est publié ici, la
 * modération arrive en phase 4. Une annonce d'un autre compte répond 404.
 */
@Service
@RequiredArgsConstructor
public class EmployerListingService {

    private static final ZoneId NIAMEY = ZoneId.of("Africa/Niamey");

    private final ListingRepository listings;
    private final AppUserRepository users;
    private final JobApplicationRepository applications;
    private final CurrentUser currentUser;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<EmployerListingResponse> list() {
        return listings.findByOwnerIdOrderByUpdatedAtDesc(currentUser.id()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EmployerListingResponse get(UUID id) {
        return toResponse(owned(id));
    }

    /** Crée une annonce, en brouillon ou soumise pour validation. */
    @Transactional
    public EmployerListingResponse create(ListingDraftRequest draft, boolean submit) {
        AppUser user = users.findById(currentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Connectez-vous pour continuer."));
        Listing listing = new Listing();
        listing.setOwner(user);
        listing.setOrganization(user.getOrganization());
        listing.setSlug(Slugs.of(draft.type().getSlugPrefix(), blank(draft.title()), blank(draft.city()), shortId(listing.getId())));
        apply(listing, draft, submit);
        return toResponse(listings.save(listing));
    }

    /** @throws ApiException 409 si l'annonce n'est plus un brouillon */
    @Transactional
    public EmployerListingResponse update(UUID id, ListingDraftRequest draft, boolean submit) {
        Listing listing = owned(id);
        if (listing.getStatus() != ListingStatus.BROUILLON) {
            throw ApiException.conflict("Seul un brouillon peut être modifié.");
        }
        apply(listing, draft, submit);
        listing.setSlug(Slugs.of(draft.type().getSlugPrefix(), blank(draft.title()), blank(draft.city()), shortId(listing.getId())));
        return toResponse(listing);
    }

    /** Retire une annonce publiée ou en attente. */
    @Transactional
    public EmployerListingResponse close(UUID id) {
        Listing listing = owned(id);
        if (listing.getStatus() != ListingStatus.PUBLIEE && listing.getStatus() != ListingStatus.EN_ATTENTE) {
            throw ApiException.conflict("Cette annonce ne peut pas être clôturée.");
        }
        return transition(listing, ListingStatus.RETIREE);
    }

    /** Renvoie une annonce clôturée en validation. */
    @Transactional
    public EmployerListingResponse republish(UUID id) {
        Listing listing = owned(id);
        if (listing.getStatus() != ListingStatus.RETIREE) {
            throw ApiException.conflict("Seule une annonce clôturée peut être republiée.");
        }
        listing.setModerationNote(null);
        return transition(listing, ListingStatus.EN_ATTENTE);
    }

    /** Crée un brouillon à partir d'une annonce existante. */
    @Transactional
    public EmployerListingResponse duplicate(UUID id) {
        Listing source = owned(id);
        Listing copy = new Listing();
        copy.setOwner(source.getOwner());
        copy.setOrganization(source.getOrganization());
        copy.setUniverse(source.getUniverse());
        copy.setType(source.getType());
        String title = source.getTitle() + " (copie)";
        copy.setTitle(title.length() > 140 ? title.substring(0, 140) : title);
        copy.setSummary(source.getSummary());
        copy.setDescription(source.getDescription());
        copy.setSectorId(source.getSectorId());
        copy.setRegionId(source.getRegionId());
        copy.setCity(source.getCity());
        copy.setDeadline(source.getDeadline());
        copy.setApplyMode(source.getApplyMode());
        copy.setApplyEmail(source.getApplyEmail());
        copy.setApplyUrl(source.getApplyUrl());
        copy.setApplyInstructions(source.getApplyInstructions());
        copy.setBudgetAmount(source.getBudgetAmount());
        copy.setBudgetCurrency(source.getBudgetCurrency());
        copy.setSlug(Slugs.of(source.getType().getSlugPrefix(), title, source.getCity(), shortId(copy.getId())));
        copy.setUpdatedAt(clock.instant());
        return toResponse(listings.save(copy));
    }

    private void apply(Listing listing, ListingDraftRequest draft, boolean submit) {
        if (submit) {
            List<String> problems = DraftRules.problems(draft, LocalDate.now(clock.withZone(NIAMEY)));
            if (!problems.isEmpty()) {
                throw ApiException.badRequest("Cette annonce n'est pas complète.", problems);
            }
        }
        listing.setType(draft.type());
        listing.setUniverse(draft.type().getUniverse());
        listing.setTitle(blank(draft.title()));
        listing.setSummary(blank(draft.summary()));
        listing.setDescription(blank(draft.description()));
        listing.setSectorId(nullIfBlank(draft.sectorId()));
        listing.setRegionId(nullIfBlank(draft.regionId()));
        listing.setCity(blank(draft.city()));
        listing.setDeadline(endOfDay(DraftRules.parseDate(draft.deadline())));
        listing.setApplyMode(draft.applyMode());
        listing.setApplyEmail(nullIfBlank(draft.applyEmail()));
        listing.setApplyUrl(nullIfBlank(draft.applyUrl()));
        listing.setApplyInstructions(blank(draft.instructions()));
        listing.setBudgetAmount(draft.budget());
        listing.setBudgetCurrency(draft.budget() == null ? null : "FCFA");
        listing.setStatus(submit ? ListingStatus.EN_ATTENTE : ListingStatus.BROUILLON);
        if (submit) {
            listing.setModerationNote(null);
        }
        listing.setUpdatedAt(clock.instant());
    }

    private EmployerListingResponse transition(Listing listing, ListingStatus status) {
        listing.setStatus(status);
        listing.setUpdatedAt(clock.instant());
        return toResponse(listing);
    }

    private Listing owned(UUID id) {
        return listings.findByIdAndOwnerId(id, currentUser.id())
                .orElseThrow(() -> ApiException.notFound("Cette annonce n'existe pas."));
    }

    private EmployerListingResponse toResponse(Listing listing) {
        Instant now = clock.instant();
        return new EmployerListingResponse(
                listing.getId(),
                listing.getOwner().getId(),
                listing.getType().name(),
                listing.getTitle(),
                listing.getSummary(),
                listing.getDescription(),
                blank(listing.getSectorId()),
                blank(listing.getRegionId()),
                listing.getCity(),
                listing.getDeadline() == null ? "" : LocalDate.ofInstant(listing.getDeadline(), NIAMEY).toString(),
                listing.getApplyMode().name(),
                blank(listing.getApplyEmail()),
                blank(listing.getApplyUrl()),
                listing.getApplyInstructions(),
                listing.getBudgetAmount(),
                ListingMapper.publicStatus(listing, now).name(),
                listing.getModerationNote(),
                listing.getViewCount(),
                applications.countByListingId(listing.getId()),
                listing.getUpdatedAt());
    }

    private static Instant endOfDay(LocalDate date) {
        return date == null ? null : date.atTime(LocalTime.of(23, 59, 59)).atZone(NIAMEY).toInstant();
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    private static String blank(String value) {
        return value == null ? "" : value.trim();
    }

    private static String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
