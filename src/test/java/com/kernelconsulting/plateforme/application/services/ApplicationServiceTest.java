package com.kernelconsulting.plateforme.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kernelconsulting.plateforme.account.domain.AppUser;
import com.kernelconsulting.plateforme.account.domain.Organization;
import com.kernelconsulting.plateforme.account.domain.OrganizationType;
import com.kernelconsulting.plateforme.account.repository.AppUserRepository;
import com.kernelconsulting.plateforme.account.services.AccountService;
import com.kernelconsulting.plateforme.application.api.dto.ApplicationResponse;
import com.kernelconsulting.plateforme.application.repository.JobApplicationRepository;
import com.kernelconsulting.plateforme.common.exception.ApiException;
import com.kernelconsulting.plateforme.common.security.CurrentUser;
import com.kernelconsulting.plateforme.common.storage.FileStorage;
import com.kernelconsulting.plateforme.common.storage.UploadValidator;
import com.kernelconsulting.plateforme.listing.domain.ApplyMode;
import com.kernelconsulting.plateforme.listing.domain.Listing;
import com.kernelconsulting.plateforme.listing.domain.ListingStatus;
import com.kernelconsulting.plateforme.listing.domain.ListingType;
import com.kernelconsulting.plateforme.listing.domain.Universe;
import com.kernelconsulting.plateforme.listing.repository.ListingRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");
    private static final String LETTER = "Je souhaite rejoindre votre équipe comptable.";
    private static final MockMultipartFile PDF = new MockMultipartFile("cv", "cv.pdf", "application/pdf", "%PDF".getBytes());

    @Mock JobApplicationRepository applications;
    @Mock ListingRepository listings;
    @Mock AppUserRepository users;
    @Mock AccountService accounts;
    @Mock FileStorage storage;
    @Mock CurrentUser currentUser;

    private ApplicationService service;
    private Listing listing;
    private AppUser user;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ApplicationService(applications, listings, users, accounts, new UploadValidator(), storage, currentUser, Clock.fixed(NOW, ZoneOffset.UTC));
        Organization organization = new Organization();
        organization.setName("ONG");
        organization.setType(OrganizationType.ONG);
        listing = new Listing();
        listing.setSlug("poste");
        listing.setUniverse(Universe.EMPLOI);
        listing.setType(ListingType.CDI);
        listing.setTitle("Poste");
        listing.setOrganization(organization);
        listing.setStatus(ListingStatus.PUBLIEE);
        listing.setApplyMode(ApplyMode.EN_LIGNE);
        listing.setDeadline(NOW.plusSeconds(3600));
        user = new AppUser();
        user.setId(userId);
        when(currentUser.id()).thenReturn(userId);
        when(users.getReferenceById(userId)).thenReturn(user);
        when(listings.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(applications.save(any())).thenAnswer(call -> call.getArgument(0));
        when(storage.store(any(), any())).thenReturn("applications/abc");
    }

    private ApplicationSubmission submission(boolean useSaved, boolean keep) {
        return new ApplicationSubmission(useSaved ? null : PDF, useSaved, keep, LETTER, true);
    }

    @Test
    void gardeUneCopieDuCvPropreALaCandidature() {
        ApplicationResponse response = service.apply(listing.getId(), submission(false, false));
        assertThat(response.documents()).containsExactly("cv.pdf");
        assertThat(response.status()).isEqualTo("ENVOYEE");
        verify(storage).store(PDF, "applications");
        verify(accounts, never()).keepCv(any(), any());
    }

    @Test
    void conserveLeFichierDansLeCompteSeulementSiDemande() {
        service.apply(listing.getId(), submission(false, true));
        verify(accounts).keepCv(user, PDF);
    }

    @Test
    void refuseSiLaDateLimiteVientDePasser() {
        listing.setDeadline(NOW.minusSeconds(1));
        assertThatThrownBy(() -> service.apply(listing.getId(), submission(false, false)))
                .isInstanceOf(ApiException.class)
                .hasMessage("La date limite de cette annonce est passée.");
        Mockito.verifyNoInteractions(storage);
    }

    @Test
    void refuseUneAnnonceRetireeCommeSiElleNExistaitPas() {
        listing.setStatus(ListingStatus.RETIREE);
        assertThatThrownBy(() -> service.apply(listing.getId(), submission(false, false)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void refuseLeDoublonAvantDeStockerLeFichier() {
        when(applications.existsByUserIdAndListingId(userId, listing.getId())).thenReturn(true);
        assertThatThrownBy(() -> service.apply(listing.getId(), submission(false, false)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        Mockito.verifyNoInteractions(storage);
    }
}
