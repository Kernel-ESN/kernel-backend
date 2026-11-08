package com.kernelconsulting.plateforme.alert;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kernelconsulting.plateforme.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

class AlertApiIT extends IntegrationTest {

    private static final String BODY = "{\"criteria\":{\"universe\":\"MARCHE\",\"sectorId\":\"informatique\"},\"channel\":\"EMAIL\",\"frequency\":\"HEBDO\"}";

    private String create(MockHttpSession session) throws Exception {
        String json = mvc.perform(post("/api/alerts").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    @Test
    void creeUneAlerteVersLEmailDuCompte() throws Exception {
        MockHttpSession session = candidate("awa@example.org");
        mvc.perform(post("/api/alerts").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("},", "},\"email\":\"tiers@example.org\",")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("awa@example.org"))
                .andExpect(jsonPath("$.criteria.universe").value("MARCHE"))
                .andExpect(jsonPath("$.criteria.sectorId").value("informatique"))
                .andExpect(jsonPath("$.criteria.type").doesNotExist());
        mvc.perform(get("/api/alerts").session(session)).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void refuseSansSession() throws Exception {
        mvc.perform(post("/api/alerts").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refuseWhatsappPourLInstant() throws Exception {
        mvc.perform(post("/api/alerts").with(csrf()).session(candidate("awa@example.org")).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("EMAIL", "WHATSAPP")))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void neMontreNiNeSupprimeLesAlertesDUnAutreCompte() throws Exception {
        MockHttpSession awa = candidate("awa@example.org");
        MockHttpSession bintou = candidate("bintou@example.org");
        String id = create(awa);
        mvc.perform(get("/api/alerts").session(bintou)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(delete("/api/alerts/" + id).with(csrf()).session(bintou)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/alerts/" + id).with(csrf()).session(awa)).andExpect(status().isNoContent());
        mvc.perform(get("/api/alerts").session(awa)).andExpect(jsonPath("$", hasSize(0)));
    }
}
