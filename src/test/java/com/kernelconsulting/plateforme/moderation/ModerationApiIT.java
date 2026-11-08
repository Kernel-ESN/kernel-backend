package com.kernelconsulting.plateforme.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kernelconsulting.plateforme.listing.domain.Listing;
import com.kernelconsulting.plateforme.listing.domain.ListingType;
import com.kernelconsulting.plateforme.moderation.domain.ModerationAction;
import com.kernelconsulting.plateforme.moderation.domain.ModerationLog;
import com.kernelconsulting.plateforme.moderation.repository.ModerationLogRepository;
import com.kernelconsulting.plateforme.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

class ModerationApiIT extends IntegrationTest {

    private static final String REASON = "{\"reason\":\"Le budget indiqué ne correspond pas au dossier.\"}";
    private static final String DRAFT = """
            {"type":"APPEL_OFFRES","title":"Audit du réseau informatique",
             "summary":"Nous cherchons un cabinet pour auditer notre réseau informatique.",
             "description":"Objet du marché : auditer le réseau informatique de nos quatre bureaux à Niamey.",
             "sectorId":"informatique","regionId":"niamey","city":"Niamey","deadline":"%s",
             "applyMode":"EMAIL","applyEmail":"offres@example.org","applyUrl":"",
             "instructions":"Envoyez votre offre.","budget":1500000}
            """.formatted(LocalDate.now().plusDays(20));

    @Autowired private ModerationLogRepository log;

    private String submit(MockHttpSession employer) throws Exception {
        String json = mvc.perform(post("/api/employer/listings").param("submit", "true").with(csrf()).session(employer)
                        .contentType(MediaType.APPLICATION_JSON).content(DRAFT))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private ResultActions act(MockHttpSession session, String path, String body) throws Exception {
        var request = post(path).with(csrf()).session(session);
        return mvc.perform(body == null ? request : request.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String organizationId(MockHttpSession moderator) throws Exception {
        String json = mvc.perform(get("/api/admin/organizations").session(moderator)).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$[0].id");
    }

    @Test
    void reserveLaModerationAuRoleAdmin() throws Exception {
        mvc.perform(get("/api/admin/listings")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/listings").session(candidate("a@example.org"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/organizations").session(employer("e@example.org"))).andExpect(status().isForbidden());
    }

    @Test
    void neLaisseJamaisUnEmployeurPublierLuiMeme() throws Exception {
        MockHttpSession employer = employer("e@example.org");
        String id = submit(employer);
        act(employer, "/api/admin/listings/" + id + "/publish", null).andExpect(status().isForbidden());
        mvc.perform(get("/api/listings").param("q", "audit")).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void publieUneAnnonceQuiDevientVisibleEtTraceLaDecision() throws Exception {
        MockHttpSession employer = employer("e@example.org");
        MockHttpSession moderator = moderator("m@example.org");
        String id = submit(employer);
        mvc.perform(get("/api/admin/listings").session(moderator))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Audit du réseau informatique"))
                .andExpect(jsonPath("$[0].submittedBy").value("e@example.org"))
                .andExpect(jsonPath("$[0].organization.verification").value("EN_ATTENTE"));

        act(moderator, "/api/admin/listings/" + id + "/publish", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLIEE"));

        mvc.perform(get("/api/listings").param("q", "audit"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].organization.verified").value(false));
        mvc.perform(get("/api/admin/listings").session(moderator)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/employer/listings/" + id).session(employer)).andExpect(jsonPath("$.status").value("PUBLIEE"));

        List<ModerationLog> entries = log.findByTargetIdOrderByCreatedAtAsc(UUID.fromString(id));
        assertThat(entries).singleElement().satisfies(entry -> {
            assertThat(entry.getAction()).isEqualTo(ModerationAction.PUBLIER);
            assertThat(entry.getModeratorId()).isEqualTo(user("m@example.org").getId());
        });
    }

    @Test
    void refuseAvecUnMotifQueLEmployeurVoitEtPeutCorriger() throws Exception {
        MockHttpSession employer = employer("e@example.org");
        MockHttpSession moderator = moderator("m@example.org");
        String id = submit(employer);
        act(moderator, "/api/admin/listings/" + id + "/reject", REASON)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BROUILLON"));
        mvc.perform(get("/api/employer/listings/" + id).session(employer))
                .andExpect(jsonPath("$.status").value("BROUILLON"))
                .andExpect(jsonPath("$.moderationNote").value("Le budget indiqué ne correspond pas au dossier."));

        mvc.perform(put("/api/employer/listings/" + id).param("submit", "true").with(csrf()).session(employer)
                        .contentType(MediaType.APPLICATION_JSON).content(DRAFT))
                .andExpect(jsonPath("$.status").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.moderationNote").doesNotExist());
    }

    @Test
    void exigeUnMotifPourRefuserOuRetirer() throws Exception {
        String id = submit(employer("e@example.org"));
        MockHttpSession moderator = moderator("m@example.org");
        act(moderator, "/api/admin/listings/" + id + "/reject", "{\"reason\":\"\"}").andExpect(status().isBadRequest());
        act(moderator, "/api/admin/listings/" + id + "/reject", "{\"reason\":\"non\"}").andExpect(status().isBadRequest());
    }

    @Test
    void refuseUneDecisionSurUneAnnonceQuiNestPlusEnAttente() throws Exception {
        String id = submit(employer("e@example.org"));
        MockHttpSession moderator = moderator("m@example.org");
        act(moderator, "/api/admin/listings/" + id + "/publish", null).andExpect(status().isOk());
        act(moderator, "/api/admin/listings/" + id + "/publish", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cette annonce n'est plus en attente."));
        act(moderator, "/api/admin/listings/" + id + "/reject", REASON).andExpect(status().isConflict());
    }

    @Test
    void retireUneAnnoncePublieeDeLaListePublique() throws Exception {
        Listing listing = published("Comptable", ListingType.CDI, "finance", "niamey");
        MockHttpSession moderator = moderator("m@example.org");
        mvc.perform(get("/api/admin/listings").param("status", "PUBLIEE").session(moderator)).andExpect(jsonPath("$", hasSize(1)));
        act(moderator, "/api/admin/listings/" + listing.getId() + "/withdraw", REASON).andExpect(jsonPath("$.status").value("RETIREE"));
        mvc.perform(get("/api/listings/EMPLOI/" + listing.getSlug())).andExpect(status().isNotFound());
        act(moderator, "/api/admin/listings/" + listing.getId() + "/withdraw", REASON).andExpect(status().isConflict());
    }

    @Test
    void accordeLeBadgeVerifieQuiApparaitSurLesAnnonces() throws Exception {
        MockHttpSession employer = employer("e@example.org");
        MockHttpSession moderator = moderator("m@example.org");
        String id = submit(employer);
        act(moderator, "/api/admin/listings/" + id + "/publish", null).andExpect(status().isOk());
        mvc.perform(get("/api/admin/organizations").session(moderator))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].contactEmail").value("e@example.org"))
                .andExpect(jsonPath("$[0].contactPhone").value("+227 90 00 00 00"))
                .andExpect(jsonPath("$[0].documentName").value("recepisse.pdf"))
                .andExpect(jsonPath("$[0].documentPath").doesNotExist());

        act(moderator, "/api/admin/organizations/" + organizationId(moderator) + "/verify", null)
                .andExpect(jsonPath("$.verification").value("VERIFIEE"));

        mvc.perform(get("/api/listings").param("q", "audit")).andExpect(jsonPath("$.items[0].organization.verified").value(true));
        mvc.perform(get("/api/me").session(employer)).andExpect(jsonPath("$.organization.verification").value("VERIFIEE"));
    }

    @Test
    void refuseUnJustificatifPuisAccepteUnNouvelEnvoi() throws Exception {
        MockHttpSession employer = employer("e@example.org");
        MockHttpSession moderator = moderator("m@example.org");
        String organization = organizationId(moderator);
        act(moderator, "/api/admin/organizations/" + organization + "/reject", REASON)
                .andExpect(jsonPath("$.verification").value("REFUSEE"));
        mvc.perform(get("/api/me").session(employer))
                .andExpect(jsonPath("$.organization.verification").value("REFUSEE"))
                .andExpect(jsonPath("$.organization.verificationNote").value("Le budget indiqué ne correspond pas au dossier."));

        mvc.perform(multipart(HttpMethod.PUT, "/api/me/organization/document")
                        .file(new MockMultipartFile("document", "arrete.pdf", "application/pdf", PDF))
                        .with(csrf()).session(employer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization.verification").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.organization.documentName").value("arrete.pdf"))
                .andExpect(jsonPath("$.organization.verificationNote").doesNotExist());
        mvc.perform(get("/api/admin/organizations").session(moderator)).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void refuseUnNouveauJustificatifUneFoisVerifieeOuSansOrganisation() throws Exception {
        MockHttpSession employer = employer("e@example.org");
        MockHttpSession moderator = moderator("m@example.org");
        act(moderator, "/api/admin/organizations/" + organizationId(moderator) + "/verify", null).andExpect(status().isOk());
        mvc.perform(multipart(HttpMethod.PUT, "/api/me/organization/document")
                        .file(new MockMultipartFile("document", "a.pdf", "application/pdf", PDF)).with(csrf()).session(employer))
                .andExpect(status().isConflict());
        mvc.perform(multipart(HttpMethod.PUT, "/api/me/organization/document")
                        .file(new MockMultipartFile("document", "a.pdf", "application/pdf", PDF)).with(csrf()).session(candidate("a@example.org")))
                .andExpect(status().isForbidden());
    }

    @Test
    void serLeJustificatifEnTelechargementAuxSeulsModerateurs() throws Exception {
        MockHttpSession employer = employer("e@example.org");
        MockHttpSession moderator = moderator("m@example.org");
        String url = "/api/admin/organizations/" + organizationId(moderator) + "/document";
        mvc.perform(get(url).session(moderator))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("recepisse.pdf")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(PDF));
        mvc.perform(get(url).session(employer)).andExpect(status().isForbidden());
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
    }
}
