package com.kernelconsulting.plateforme.employer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kernelconsulting.plateforme.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

class EmployerApiIT extends IntegrationTest {

    private static final String URL = "/api/employer/listings";

    private static String complete() {
        return """
                {"type":"APPEL_OFFRES","title":"Audit du réseau informatique",
                 "summary":"Nous cherchons un cabinet pour auditer notre réseau informatique.",
                 "description":"Objet du marché : auditer le réseau informatique de nos quatre bureaux à Niamey.",
                 "sectorId":"informatique","regionId":"niamey","city":"Niamey","deadline":"%s",
                 "applyMode":"EMAIL","applyEmail":"offres@example.org","applyUrl":"",
                 "instructions":"Envoyez votre offre.","budget":1500000}
                """.formatted(LocalDate.now().plusDays(20));
    }

    private String create(MockHttpSession session, String body, boolean submit) throws Exception {
        String json = mvc.perform(post(URL).param("submit", String.valueOf(submit)).with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    @Test
    void soumetUneAnnonceEnAttenteSansLaPublier() throws Exception {
        MockHttpSession session = employer("ong@example.org");
        mvc.perform(post(URL).param("submit", "true").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON).content(complete()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.title").value("Audit du réseau informatique"))
                .andExpect(jsonPath("$.views").value(0))
                .andExpect(jsonPath("$.applications").value(0));
        mvc.perform(get(URL).session(session)).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/listings").param("q", "audit")).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void gardeUnBrouillonIncompletMaisRefuseDeLeSoumettre() throws Exception {
        MockHttpSession session = employer("ong@example.org");
        String draft = "{\"type\":\"CDI\",\"title\":\"\",\"summary\":\"\",\"description\":\"\",\"sectorId\":\"\",\"regionId\":\"\",\"city\":\"\",\"deadline\":\"\",\"applyMode\":\"EN_LIGNE\",\"applyEmail\":\"\",\"applyUrl\":\"\",\"instructions\":\"\",\"budget\":null}";
        String id = create(session, draft, false);
        mvc.perform(get(URL + "/" + id).session(session))
                .andExpect(jsonPath("$.status").value("BROUILLON"))
                .andExpect(jsonPath("$.deadline").value(""))
                .andExpect(jsonPath("$.sectorId").value(""));
        mvc.perform(put(URL + "/" + id).param("submit", "true").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON).content(draft))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cette annonce n'est pas complète."))
                .andExpect(jsonPath("$.details", hasItem("Choisissez un secteur.")));
    }

    @Test
    void passeUnBrouillonEnValidationPuisRefuseDeLeModifier() throws Exception {
        MockHttpSession session = employer("ong@example.org");
        String id = create(session, complete(), false);
        mvc.perform(put(URL + "/" + id).param("submit", "true").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON).content(complete()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_ATTENTE"));
        mvc.perform(put(URL + "/" + id).with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON).content(complete()))
                .andExpect(status().isConflict());
    }

    @Test
    void clotureRepublieEtDuplique() throws Exception {
        MockHttpSession session = employer("ong@example.org");
        String id = create(session, complete(), true);
        mvc.perform(post(URL + "/" + id + "/close").with(csrf()).session(session)).andExpect(jsonPath("$.status").value("RETIREE"));
        mvc.perform(post(URL + "/" + id + "/close").with(csrf()).session(session)).andExpect(status().isConflict());
        mvc.perform(post(URL + "/" + id + "/republish").with(csrf()).session(session)).andExpect(jsonPath("$.status").value("EN_ATTENTE"));
        mvc.perform(post(URL + "/" + id + "/duplicate").with(csrf()).session(session))
                .andExpect(jsonPath("$.status").value("BROUILLON"))
                .andExpect(jsonPath("$.title").value("Audit du réseau informatique (copie)"))
                .andExpect(jsonPath("$.applyEmail").value("offres@example.org"));
        mvc.perform(get(URL).session(session)).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void cacheLesAnnoncesDUneAutreOrganisation() throws Exception {
        String id = create(employer("a@example.org"), complete(), true);
        MockHttpSession other = employer("b@example.org");
        mvc.perform(get(URL + "/" + id).session(other)).andExpect(status().isNotFound());
        mvc.perform(post(URL + "/" + id + "/close").with(csrf()).session(other)).andExpect(status().isNotFound());
        mvc.perform(get(URL).session(other)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rattacheLAnnonceALOrganisationDuCompte() throws Exception {
        String id = create(employer("ong@example.org"), complete(), true);
        assertThat(listings.findAll()).singleElement()
                .satisfies(listing -> {
                    assertThat(listing.getId().toString()).isEqualTo(id);
                    assertThat(listing.getSlug()).startsWith("ao-audit-du-reseau-informatique-niamey-");
                });
    }

    @Test
    void refuseUnCandidatEtUnVisiteur() throws Exception {
        mvc.perform(get(URL).session(candidate("awa@example.org")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Vous n'avez pas accès à cette ressource."));
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }
}
