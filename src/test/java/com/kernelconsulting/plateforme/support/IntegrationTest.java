package com.kernelconsulting.plateforme.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kernelconsulting.plateforme.account.domain.AppUser;
import com.kernelconsulting.plateforme.account.domain.Organization;
import com.kernelconsulting.plateforme.account.domain.OrganizationType;
import com.kernelconsulting.plateforme.account.domain.Verification;
import com.kernelconsulting.plateforme.account.repository.AppUserRepository;
import com.kernelconsulting.plateforme.account.repository.OrganizationRepository;
import com.kernelconsulting.plateforme.listing.domain.ApplyMode;
import com.kernelconsulting.plateforme.listing.domain.Listing;
import com.kernelconsulting.plateforme.listing.domain.ListingStatus;
import com.kernelconsulting.plateforme.listing.domain.ListingType;
import com.kernelconsulting.plateforme.listing.repository.ListingRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Pile complète contre un vrai PostgreSQL : sécurité, validation, recherche et contraintes comprises. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    /** Un seul conteneur pour toute la suite : le contexte Spring est partagé entre les classes. */
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("docker.io/library/postgres:18-alpine").asCompatibleSubstituteFor("postgres"));

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    protected static final String PASSWORD = "motdepasse";
    protected static final byte[] PDF = "%PDF-1.4 contenu".getBytes();

    @Autowired protected MockMvc mvc;
    @Autowired protected ListingRepository listings;
    @Autowired protected OrganizationRepository organizations;
    @Autowired protected AppUserRepository users;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder encoder;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE moderation_log, alert, application, saved_listing, listing_attachment, listing, app_user, organization CASCADE");
    }

    /** Inscrit un candidat et renvoie sa session. */
    protected MockHttpSession candidate(String email) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/register").with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Awa\",\"email\":\"%s\",\"password\":\"%s\",\"role\":\"CANDIDAT\"}".formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
        return session;
    }

    /** Inscrit un employeur avec son justificatif et renvoie sa session. */
    protected MockHttpSession employer(String email) throws Exception {
        MockHttpSession session = new MockHttpSession();
        String data = "{\"name\":\"Awa\",\"email\":\"%s\",\"password\":\"%s\",\"role\":\"EMPLOYEUR\",\"phone\":\"+227 90 00 00 00\",\"organization\":{\"name\":\"ONG %s\",\"type\":\"ONG\"}}"
                .formatted(email, PASSWORD, email);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/auth/register")
                        .file(new MockMultipartFile("document", "recepisse.pdf", "application/pdf", PDF))
                        .param("data", data)
                        .with(csrf()).session(session))
                .andExpect(status().isCreated());
        return session;
    }

    /** Crée un modérateur en base (l'inscription publique ne donne pas ce rôle) et renvoie sa session. */
    protected MockHttpSession moderator(String email) throws Exception {
        AppUser admin = new AppUser();
        admin.setEmail(email);
        admin.setName("Modération");
        admin.setRole(com.kernelconsulting.plateforme.account.domain.UserRole.ADMIN);
        admin.setPasswordHash(encoder.encode(PASSWORD));
        users.save(admin);
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/login").with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isOk());
        return session;
    }

    protected Organization organization(String name) {
        Organization organization = new Organization();
        organization.setName(name);
        organization.setType(OrganizationType.ONG);
        organization.setVerification(Verification.VERIFIEE);
        return organizations.save(organization);
    }

    /** Annonce publiée, active pendant dix jours. */
    protected Listing published(String title, ListingType type, String sector, String region) {
        Listing listing = new Listing();
        listing.setSlug("annonce-" + UUID.randomUUID());
        listing.setType(type);
        listing.setUniverse(type.getUniverse());
        listing.setTitle(title);
        listing.setSummary("Résumé de " + title);
        listing.setDescription("Premier paragraphe.\n\nDeuxième paragraphe.");
        listing.setOrganization(organization("Organisation " + UUID.randomUUID()));
        listing.setSectorId(sector);
        listing.setRegionId(region);
        listing.setCity("Niamey");
        listing.setStatus(ListingStatus.PUBLIEE);
        listing.setPublishedAt(Instant.now().minus(Duration.ofDays(1)));
        listing.setDeadline(Instant.now().plus(Duration.ofDays(10)));
        listing.setApplyMode(ApplyMode.EN_LIGNE);
        listing.setApplyInstructions("Postulez en ligne.");
        return listings.save(listing);
    }

    protected Listing withBudget(Listing listing, long amount, boolean hidden) {
        listing.setBudgetAmount(BigDecimal.valueOf(amount));
        listing.setBudgetCurrency("FCFA");
        listing.setBudgetHidden(hidden);
        return listings.save(listing);
    }

    protected AppUser user(String email) {
        return users.findByEmail(email).orElseThrow();
    }
}
