package com.kernelconsulting.plateforme.dev;

import com.kernelconsulting.plateforme.account.domain.AppUser;
import com.kernelconsulting.plateforme.account.domain.Organization;
import com.kernelconsulting.plateforme.account.domain.OrganizationType;
import com.kernelconsulting.plateforme.account.domain.UserRole;
import com.kernelconsulting.plateforme.account.domain.Verification;
import com.kernelconsulting.plateforme.account.repository.AppUserRepository;
import com.kernelconsulting.plateforme.account.repository.OrganizationRepository;
import com.kernelconsulting.plateforme.listing.domain.ApplyMode;
import com.kernelconsulting.plateforme.listing.domain.Listing;
import com.kernelconsulting.plateforme.listing.domain.ListingAttachment;
import com.kernelconsulting.plateforme.listing.domain.ListingStatus;
import com.kernelconsulting.plateforme.listing.domain.ListingType;
import com.kernelconsulting.plateforme.listing.repository.ListingRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Annonces fictives et comptes d'essai, pour le développement uniquement. Les dates repartent
 * d'aujourd'hui à chaque démarrage. Ne s'exécute jamais hors du profil {@code dev}.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    private final ListingRepository listings;
    private final OrganizationRepository organizations;
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper mapper;
    private final Clock clock;

    record Row(
            String slug, String universe, String type, String reference, String title, String summary,
            List<String> description, String organizationName, String organizationType, boolean verified,
            String sectorId, String regionId, String city, String educationLevel, String experienceLevel,
            long publishedOffsetMs, long deadlineOffsetMs, String applyMode, String applyEmail, String applyUrl,
            String instructions, Long budget, boolean budgetHidden, List<Attachment> attachments) {
    }

    record Attachment(String name, String url, long sizeBytes) {
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Instant today = clock.instant().truncatedTo(ChronoUnit.DAYS);
        Row[] rows;
        try (InputStream in = new ClassPathResource("dev/sample-listings.json").getInputStream()) {
            rows = mapper.readValue(in, Row[].class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (Row row : rows) {
            upsert(row, today);
        }
        account("candidat@example.org", "Candidat de démonstration", UserRole.CANDIDAT, null);
        account("employeur@example.org", "Employeur de démonstration", UserRole.EMPLOYEUR, organization("Organisation Exemple", "ONG", true));
        account("moderateur@example.org", "Modérateur de démonstration", UserRole.ADMIN, null);
        log.info("Données d'exemple chargées : {} annonces fictives", rows.length);
    }

    private void upsert(Row row, Instant today) {
        Listing listing = listings.findBySlug(row.slug()).orElseGet(Listing::new);
        listing.setSlug(row.slug());
        ListingType type = ListingType.valueOf(row.type());
        listing.setType(type);
        listing.setUniverse(type.getUniverse());
        listing.setReference(row.reference());
        listing.setTitle(row.title());
        listing.setSummary(row.summary().length() > 300 ? row.summary().substring(0, 300) : row.summary());
        listing.setDescription(String.join("\n\n", row.description()));
        listing.setOrganization(organization(row.organizationName(), row.organizationType(), row.verified()));
        listing.setSectorId(row.sectorId());
        listing.setRegionId(row.regionId());
        listing.setCity(row.city());
        listing.setEducationLevel(row.educationLevel());
        listing.setExperienceLevel(row.experienceLevel());
        listing.setPublishedAt(today.plus(Duration.ofMillis(row.publishedOffsetMs())));
        listing.setDeadline(today.plus(Duration.ofMillis(row.deadlineOffsetMs())));
        listing.setStatus(ListingStatus.PUBLIEE);
        listing.setApplyMode(ApplyMode.valueOf(row.applyMode()));
        listing.setApplyEmail(row.applyEmail());
        listing.setApplyUrl(row.applyUrl());
        listing.setApplyInstructions(row.instructions());
        listing.setBudgetAmount(row.budget() == null ? null : BigDecimal.valueOf(row.budget()));
        listing.setBudgetCurrency(row.budget() == null ? null : "FCFA");
        listing.setBudgetHidden(row.budgetHidden());
        listing.setSample(true);
        listing.setUpdatedAt(clock.instant());
        listing.getAttachments().clear();
        row.attachments().forEach(file ->
                listing.getAttachments().add(new ListingAttachment(file.name(), file.url(), file.sizeBytes())));
        listings.save(listing);
    }

    private Organization organization(String name, String type, boolean verified) {
        return organizations.findFirstByName(name).orElseGet(() -> {
            Organization organization = new Organization();
            organization.setName(name);
            organization.setType(OrganizationType.valueOf(type));
            organization.setVerification(verified ? Verification.VERIFIEE : Verification.EN_ATTENTE);
            return organizations.save(organization);
        });
    }

    private void account(String email, String name, UserRole role, Organization organization) {
        if (users.existsByEmail(email)) {
            return;
        }
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setName(name);
        user.setRole(role);
        user.setOrganization(organization);
        user.setPasswordHash(passwordEncoder.encode("motdepasse"));
        users.save(user);
    }
}
