package com.kernelconsulting.plateforme.application.services;

import com.kernelconsulting.plateforme.account.domain.AppUser;
import com.kernelconsulting.plateforme.account.repository.AppUserRepository;
import com.kernelconsulting.plateforme.account.services.AccountService;
import com.kernelconsulting.plateforme.application.api.dto.ApplicationResponse;
import com.kernelconsulting.plateforme.application.domain.JobApplication;
import com.kernelconsulting.plateforme.application.repository.JobApplicationRepository;
import com.kernelconsulting.plateforme.common.exception.ApiException;
import com.kernelconsulting.plateforme.common.security.CurrentUser;
import com.kernelconsulting.plateforme.common.storage.FileKind;
import com.kernelconsulting.plateforme.common.storage.FileStorage;
import com.kernelconsulting.plateforme.common.storage.UploadValidator;
import com.kernelconsulting.plateforme.listing.api.mapper.ListingMapper;
import com.kernelconsulting.plateforme.listing.domain.ApplyMode;
import com.kernelconsulting.plateforme.listing.domain.Listing;
import com.kernelconsulting.plateforme.listing.domain.ListingStatus;
import com.kernelconsulting.plateforme.listing.repository.ListingRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Candidatures en ligne de l'utilisateur connecté. */
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final int MIN_LETTER = 20;
    private static final int MAX_LETTER = 4000;

    private final JobApplicationRepository applications;
    private final ListingRepository listings;
    private final AppUserRepository users;
    private final AccountService accounts;
    private final UploadValidator uploads;
    private final FileStorage storage;
    private final CurrentUser currentUser;
    private final Clock clock;

    /** @return les candidatures de la session, les plus récentes d'abord */
    @Transactional(readOnly = true)
    public List<ApplicationResponse> mine() {
        return applications.findMine(currentUser.id()).stream().map(ApplicationService::toResponse).toList();
    }

    /**
     * Envoie une candidature. Elle garde sa propre copie du CV : la supprimer du compte ne la touche pas.
     *
     * @throws ApiException 400 sans accord ou lettre valide, 404 si l'annonce n'est pas publiée,
     *     409 en cas de doublon, 422 si l'annonce n'accepte plus de candidature ou si le fichier est refusé
     */
    @Transactional
    public ApplicationResponse apply(UUID listingId, ApplicationSubmission submission) {
        checkContent(submission);
        Listing listing = listings.findById(listingId)
                .filter(found -> found.getStatus() == ListingStatus.PUBLIEE)
                .orElseThrow(() -> ApiException.notFound("Cette annonce n'existe pas ou a été retirée."));
        if (listing.getApplyMode() != ApplyMode.EN_LIGNE) {
            throw ApiException.unprocessable("Cette annonce n'accepte pas de candidature en ligne.");
        }
        if (listing.isExpiredAt(clock.instant())) {
            throw ApiException.unprocessable("La date limite de cette annonce est passée.");
        }
        UUID userId = currentUser.id();
        if (applications.existsByUserIdAndListingId(userId, listingId)) {
            throw ApiException.conflict("Vous avez déjà postulé à cette annonce.");
        }
        AppUser user = users.getReferenceById(userId);
        JobApplication application = new JobApplication();
        application.setUser(user);
        application.setListing(listing);
        application.setLetter(submission.letter().trim());
        attachCv(application, user, submission);
        return toResponse(applications.save(application));
    }

    private void attachCv(JobApplication application, AppUser user, ApplicationSubmission submission) {
        if (submission.useSavedCv()) {
            if (user.getCvPath() == null) {
                throw ApiException.unprocessable("Aucun CV n'est enregistré dans votre compte.");
            }
            application.setCvPath(user.getCvPath());
            application.setCvName(user.getCvName());
            return;
        }
        uploads.check(submission.cv(), FileKind.CV);
        application.setCvPath(storage.store(submission.cv(), "applications"));
        application.setCvName(submission.cv().getOriginalFilename());
        if (submission.keepCv()) {
            accounts.keepCv(user, submission.cv());
        }
    }

    private static void checkContent(ApplicationSubmission submission) {
        if (!submission.consent()) {
            throw ApiException.badRequest("Votre accord est nécessaire pour envoyer la candidature.", List.of());
        }
        String letter = submission.letter() == null ? "" : submission.letter().trim();
        if (letter.length() < MIN_LETTER || letter.length() > MAX_LETTER) {
            throw ApiException.badRequest("La lettre doit compter entre 20 et 4000 caractères.", List.of());
        }
    }

    private static ApplicationResponse toResponse(JobApplication application) {
        return new ApplicationResponse(
                application.getId(),
                ListingMapper.toSaved(application.getListing()),
                application.getUser().getId(),
                application.getStatus().name(),
                application.getCreatedAt(),
                List.of(application.getCvName()));
    }
}
