package com.kernelconsulting.plateforme.listing;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kernelconsulting.plateforme.listing.domain.Listing;
import com.kernelconsulting.plateforme.listing.domain.ListingType;
import com.kernelconsulting.plateforme.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class SavedListingApiIT extends IntegrationTest {

    @Test
    void enregistreUneFoisPuisRetire() throws Exception {
        Listing listing = published("Comptable", ListingType.CDI, "finance", "niamey");
        MockHttpSession session = candidate("awa@example.org");
        String url = "/api/me/saved/" + listing.getId();
        mvc.perform(put(url).with(csrf()).session(session)).andExpect(status().isNoContent());
        mvc.perform(put(url).with(csrf()).session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/me/saved").session(session))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Comptable"))
                .andExpect(jsonPath("$[0].organizationName").value(org.hamcrest.Matchers.startsWith("Organisation ")));
        mvc.perform(delete(url).with(csrf()).session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/me/saved").session(session)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void repond404SurUneAnnonceInconnueEt401SansSession() throws Exception {
        mvc.perform(put("/api/me/saved/00000000-0000-0000-0000-000000000000").with(csrf()).session(candidate("awa@example.org")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/me/saved")).andExpect(status().isUnauthorized());
    }
}
