package com.kernelconsulting.plateforme.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.kernelconsulting.plateforme.account.domain.AppUser;
import com.kernelconsulting.plateforme.account.domain.UserRole;
import com.kernelconsulting.plateforme.support.IntegrationTest;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Vrais appels HTTP, avec les cookies et l'en-tête que le navigateur et Angular envoient. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CsrfOverHttpIT extends IntegrationTest {

    private static final String LOGIN = "{\"email\":\"awa@example.org\",\"password\":\"motdepasse\"}";

    @LocalServerPort private int port;
    @Autowired private PasswordEncoder encoder;

    private CookieManager cookies;
    private HttpClient client;

    @BeforeEach
    void browser() {
        cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        client = HttpClient.newBuilder().cookieHandler(cookies).build();
    }

    /**
     * Compte créé directement en base. {@code with(csrf())} remplace le dépôt de jetons du filtre
     * partagé : l'utiliser ici fausserait justement ce que ce test vérifie.
     */
    private void account() {
        AppUser user = new AppUser();
        user.setEmail("awa@example.org");
        user.setName("Awa");
        user.setRole(UserRole.CANDIDAT);
        user.setPasswordHash(encoder.encode(PASSWORD));
        users.save(user);
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest post(String path, String body, String token) {
        HttpRequest.Builder builder = request(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        return (token == null ? builder : builder.header("X-XSRF-TOKEN", token)).build();
    }

    private HttpCookie cookie(String name) {
        return cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Test
    void deposeUnCookieLisibleParAngularPuisAccepteLEnvoi() throws Exception {
        account();
        assertThat(send(request("/api/auth/csrf").GET().build()).statusCode()).isEqualTo(204);
        HttpCookie xsrf = cookie("XSRF-TOKEN");
        assertThat(xsrf).isNotNull();
        assertThat(xsrf.isHttpOnly()).isFalse();

        HttpResponse<String> login = send(post("/api/auth/login", LOGIN, xsrf.getValue()));
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body()).contains("awa@example.org");

        HttpCookie session = cookie("JSESSIONID");
        assertThat(session).isNotNull();
        assertThat(session.isHttpOnly()).isTrue();
        assertThat(send(request("/api/me").GET().build()).statusCode()).isEqualTo(200);
    }

    @Test
    void laSessionResteValableApresLaConnexionPourLesEnvoisSuivants() throws Exception {
        account();
        send(request("/api/auth/csrf").GET().build());
        String token = cookie("XSRF-TOKEN").getValue();
        send(post("/api/auth/login", LOGIN, token));

        HttpResponse<String> alert = send(post(
                "/api/alerts", "{\"criteria\":{},\"channel\":\"EMAIL\",\"frequency\":\"HEBDO\"}", cookie("XSRF-TOKEN").getValue()));
        assertThat(alert.statusCode()).isEqualTo(201);

        assertThat(send(post("/api/auth/logout", "", cookie("XSRF-TOKEN").getValue())).statusCode()).isEqualTo(204);
        assertThat(send(request("/api/me").GET().build()).statusCode()).isEqualTo(401);
    }

    @Test
    void refuseUnEnvoiSansEnTeteOuAvecUnFauxJeton() throws Exception {
        account();
        send(request("/api/auth/csrf").GET().build());
        assertThat(send(post("/api/auth/login", LOGIN, null)).statusCode()).isEqualTo(403);
        assertThat(send(post("/api/auth/login", LOGIN, "faux-jeton")).statusCode()).isEqualTo(403);
    }
}
