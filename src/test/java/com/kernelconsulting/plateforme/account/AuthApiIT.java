package com.kernelconsulting.plateforme.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kernelconsulting.plateforme.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;

class AuthApiIT extends IntegrationTest {

    private static String login(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }

    @Test
    void inscritUnCandidatEtOuvreSaSession() throws Exception {
        MockHttpSession session = candidate("Awa@Example.org");
        mvc.perform(get("/api/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("awa@example.org"))
                .andExpect(jsonPath("$.role").value("CANDIDAT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
        assertThat(user("awa@example.org").getPasswordHash()).doesNotContain(PASSWORD);
    }

    @Test
    void refuseUnEmailDejaPris() throws Exception {
        candidate("awa@example.org");
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"B\",\"email\":\"AWA@example.org\",\"password\":\"motdepasse\",\"role\":\"CANDIDAT\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Un compte existe déjà avec cet e-mail."));
    }

    @Test
    void refuseUnMotDePasseCourtEtExpliquePourquoi() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"B\",\"email\":\"b@example.org\",\"password\":\"court\",\"role\":\"CANDIDAT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.startsWith("password")));
    }

    @Test
    void neDonneJamaisLeRoleAdministrateurALInscription() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"B\",\"email\":\"b@example.org\",\"password\":\"motdepasse\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
        assertThat(users.existsByEmail("b@example.org")).isFalse();
    }

    @Test
    void connecteAvecLeBonMotDePasseSeulement() throws Exception {
        candidate("awa@example.org");
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(login("awa@example.org", "mauvais-mot")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou mot de passe incorrect."));
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(login("AWA@example.org", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("awa@example.org"));
    }

    @Test
    void neDistinguePasUnCompteInconnuDUnMauvaisMotDePasse() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(login("personne@example.org", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou mot de passe incorrect."));
    }

    @Test
    void fermeLaSession() throws Exception {
        MockHttpSession session = candidate("awa@example.org");
        mvc.perform(post("/api/auth/logout").with(csrf()).session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void refuseUnAccesSansSessionAvecUnMessageLisible() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Connectez-vous pour continuer."));
    }

    @Test
    void refuseUnEnvoiSansJetonCsrf() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login("a@example.org", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void inscritUneOrganisationEnAttenteDeVerification() throws Exception {
        MockHttpSession session = employer("ong@example.org");
        mvc.perform(get("/api/me").session(session))
                .andExpect(jsonPath("$.role").value("EMPLOYEUR"))
                .andExpect(jsonPath("$.phone").value("+227 90 00 00 00"))
                .andExpect(jsonPath("$.organization.verification").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.organization.documentName").value("recepisse.pdf"))
                .andExpect(jsonPath("$.organization.documentPath").doesNotExist());
    }

    @Test
    void refuseUnFauxPdfDAprèsSonContenu() throws Exception {
        String data = "{\"name\":\"A\",\"email\":\"a@example.org\",\"password\":\"motdepasse\",\"role\":\"EMPLOYEUR\",\"phone\":\"+227 90 00 00 00\",\"organization\":{\"name\":\"O\",\"type\":\"ONG\"}}";
        mvc.perform(multipart("/api/auth/register")
                        .file(new MockMultipartFile("document", "recepisse.pdf", "application/pdf", "MZ programme".getBytes()))
                        .param("data", data).with(csrf()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Ce format n'est pas accepté. Utilisez un PDF, un JPEG ou un PNG."));
        assertThat(users.existsByEmail("a@example.org")).isFalse();
    }

    @Test
    void exigeUnJustificatifPourUnEmployeur() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"email\":\"a@example.org\",\"password\":\"motdepasse\",\"role\":\"EMPLOYEUR\",\"phone\":\"+227 90 00 00 00\",\"organization\":{\"name\":\"O\",\"type\":\"ONG\"}}"))
                .andExpect(status().isUnprocessableContent());
        assertThat(users.existsByEmail("a@example.org")).isFalse();
    }

    @Test
    void exigeLeNomEtLeTelephoneDeLOrganisation() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"email\":\"a@example.org\",\"password\":\"motdepasse\",\"role\":\"EMPLOYEUR\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void repondSurLeControleDeSante() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}
