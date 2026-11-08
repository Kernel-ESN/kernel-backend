package com.kernelconsulting.plateforme.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kernelconsulting.plateforme.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.RequestBuilder;

class MeApiIT extends IntegrationTest {

    private RequestBuilder putCv(byte[] content, MockHttpSession session) {
        return multipart(HttpMethod.PUT, "/api/me/cv")
                .file(new MockMultipartFile("cv", "cv.pdf", "application/pdf", content))
                .with(csrf()).session(session);
    }

    @Test
    void modifieLeProfilSansToucherALEmail() throws Exception {
        MockHttpSession session = candidate("awa@example.org");
        mvc.perform(put("/api/me").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" Awa Moussa \",\"phone\":\"+227 90 00 00 00\",\"email\":\"pirate@example.org\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Awa Moussa"))
                .andExpect(jsonPath("$.phone").value("+227 90 00 00 00"))
                .andExpect(jsonPath("$.email").value("awa@example.org"));
    }

    @Test
    void refuseUnTelephoneInvalideEtAccepteUnTelephoneVide() throws Exception {
        MockHttpSession session = candidate("awa@example.org");
        mvc.perform(put("/api/me").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Awa\",\"phone\":\"abc\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/me").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Awa\",\"phone\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").doesNotExist());
    }

    @Test
    void enregistreRemplaceEtSupprimeLeCv() throws Exception {
        MockHttpSession session = candidate("awa@example.org");
        mvc.perform(get("/api/me/cv").session(session)).andExpect(status().isNoContent());
        mvc.perform(putCv(PDF, session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("cv.pdf"))
                .andExpect(jsonPath("$.sizeBytes").value(PDF.length))
                .andExpect(jsonPath("$.path").doesNotExist());
        String first = user("awa@example.org").getCvPath();
        mvc.perform(putCv(PDF, session)).andExpect(status().isOk());
        assertThat(user("awa@example.org").getCvPath()).isNotEqualTo(first);
        mvc.perform(delete("/api/me/cv").with(csrf()).session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/me/cv").session(session)).andExpect(status().isNoContent());
    }

    @Test
    void refuseUnCvQuiNEstPasUnDocument() throws Exception {
        MockHttpSession session = candidate("awa@example.org");
        mvc.perform(putCv("MZ programme".getBytes(), session))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Ce format n'est pas accepté. Utilisez un PDF ou un fichier Word."));
    }

    @Test
    void refuseLeCvSansSession() throws Exception {
        mvc.perform(get("/api/me/cv")).andExpect(status().isUnauthorized());
    }
}
