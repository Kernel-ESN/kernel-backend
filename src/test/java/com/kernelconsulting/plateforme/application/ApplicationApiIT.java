package com.kernelconsulting.plateforme.application;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kernelconsulting.plateforme.listing.domain.ApplyMode;
import com.kernelconsulting.plateforme.listing.domain.Listing;
import com.kernelconsulting.plateforme.listing.domain.ListingType;
import com.kernelconsulting.plateforme.support.IntegrationTest;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

class ApplicationApiIT extends IntegrationTest {

    private static final String LETTER = "Je souhaite rejoindre votre équipe comptable.";

    private MockMultipartHttpServletRequestBuilder apply(Listing listing, MockHttpSession session) {
        return (MockMultipartHttpServletRequestBuilder) multipart("/api/listings/" + listing.getId() + "/applications")
                .param("letter", LETTER).param("consent", "true").with(csrf()).session(session);
    }

    private static MockMultipartFile cv(String name) {
        return new MockMultipartFile("cv", name, "application/pdf", PDF);
    }

    @Test
    void envoieUneCandidatureEtLaRetrouve() throws Exception {
        Listing listing = published("Comptable", ListingType.CDI, "finance", "niamey");
        MockHttpSession session = candidate("awa@example.org");
        mvc.perform(apply(listing, session).file(cv("cv.pdf")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ENVOYEE"))
                .andExpect(jsonPath("$.listing.id").value(listing.getId().toString()))
                .andExpect(jsonPath("$.documents", contains("cv.pdf")));
        mvc.perform(get("/api/me/applications").session(session))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].listing.title").value("Comptable"));
    }

    @Test
    void refuseSansSession() throws Exception {
        Listing listing = published("Comptable", ListingType.CDI, "finance", "niamey");
        mvc.perform(multipart("/api/listings/" + listing.getId() + "/applications").file(cv("cv.pdf")).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refuseUnDoublon() throws Exception {
        Listing listing = published("Comptable", ListingType.CDI, "finance", "niamey");
        MockHttpSession session = candidate("awa@example.org");
        mvc.perform(apply(listing, session).file(cv("cv.pdf"))).andExpect(status().isCreated());
        mvc.perform(apply(listing, session).file(cv("cv.pdf")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Vous avez déjà postulé à cette annonce."));
    }

    @Test
    void reprendLeCvEnregistreSansFichierJoint() throws Exception {
        Listing listing = published("Comptable", ListingType.CDI, "finance", "niamey");
        MockHttpSession session = candidate("awa@example.org");
        mvc.perform(multipart(HttpMethod.PUT, "/api/me/cv").file(cv("mon-cv.pdf")).with(csrf()).session(session))
                .andExpect(status().isOk());
        mvc.perform(apply(listing, session).param("useSavedCv", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documents", contains("mon-cv.pdf")));
    }

    @Test
    void laCandidatureResteLisibleApresSuppressionDuCvDuCompte() throws Exception {
        Listing listing = published("Comptable", ListingType.CDI, "finance", "niamey");
        MockHttpSession session = candidate("awa@example.org");
        mvc.perform(apply(listing, session).file(cv("cv.pdf")).param("keepCv", "true")).andExpect(status().isCreated());
        mvc.perform(get("/api/me/cv").session(session)).andExpect(jsonPath("$.name").value("cv.pdf"));
        mvc.perform(delete("/api/me/cv").with(csrf()).session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/me/applications").session(session)).andExpect(jsonPath("$[0].documents", contains("cv.pdf")));
    }

    @Test
    void refuseDeReprendreUnCvInexistant() throws Exception {
        Listing listing = published("Comptable", ListingType.CDI, "finance", "niamey");
        MockHttpSession session = candidate("awa@example.org");
        mvc.perform(apply(listing, session).param("useSavedCv", "true"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Aucun CV n'est enregistré dans votre compte."));
    }

    @Test
    void refuseSansFichierNiCvEnregistre() throws Exception {
        Listing listing = published("Comptable", ListingType.CDI, "finance", "niamey");
        mvc.perform(apply(listing, candidate("awa@example.org"))).andExpect(status().isUnprocessableContent());
    }

    @Test
    void exigeLAccordEtUneLettre() throws Exception {
        Listing listing = published("Comptable", ListingType.CDI, "finance", "niamey");
        MockHttpSession session = candidate("awa@example.org");
        mvc.perform(multipart("/api/listings/" + listing.getId() + "/applications").file(cv("cv.pdf"))
                        .param("letter", LETTER).with(csrf()).session(session))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/listings/" + listing.getId() + "/applications").file(cv("cv.pdf"))
                        .param("letter", "court").param("consent", "true").with(csrf()).session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refuseUneAnnonceParEmailOuExpiree() throws Exception {
        MockHttpSession session = candidate("awa@example.org");
        Listing byEmail = published("Par e-mail", ListingType.CDI, "finance", "niamey");
        byEmail.setApplyMode(ApplyMode.EMAIL);
        listings.save(byEmail);
        mvc.perform(apply(byEmail, session).file(cv("cv.pdf"))).andExpect(status().isUnprocessableContent());
        Listing expired = published("Expirée", ListingType.CDI, "finance", "niamey");
        expired.setDeadline(Instant.now().minus(Duration.ofDays(1)));
        listings.save(expired);
        mvc.perform(apply(expired, session).file(cv("cv.pdf")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("La date limite de cette annonce est passée."));
    }
}
