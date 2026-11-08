package com.kernelconsulting.plateforme.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthRateLimitFilterTest {

    private final AuthRateLimitFilter filter = new AuthRateLimitFilter(new RateLimiter(1, Clock.systemUTC()));

    private MockHttpServletResponse send(String method, String path) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(method, path), response, new MockFilterChain());
        return response;
    }

    @Test
    void bloqueLeDeuxiemeEssaiDeConnexionAvecUnMessageLisible() throws Exception {
        assertThat(send("POST", "/api/auth/login").getStatus()).isEqualTo(200);
        MockHttpServletResponse blocked = send("POST", "/api/auth/login");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getContentAsString()).contains("Réessayez dans une minute");
    }

    @Test
    void compteLInscriptionAvecLaConnexion() throws Exception {
        send("POST", "/api/auth/login");
        assertThat(send("POST", "/api/auth/register").getStatus()).isEqualTo(429);
    }

    @Test
    void neLimitePasLesAutresAppels() throws Exception {
        send("POST", "/api/auth/login");
        assertThat(send("GET", "/api/auth/csrf").getStatus()).isEqualTo(200);
        assertThat(send("POST", "/api/alerts").getStatus()).isEqualTo(200);
    }
}
