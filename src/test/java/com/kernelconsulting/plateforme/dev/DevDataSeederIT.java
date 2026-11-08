package com.kernelconsulting.plateforme.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.kernelconsulting.plateforme.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "dev"})
class DevDataSeederIT extends IntegrationTest {

    @Autowired private DevDataSeeder seeder;
    @Autowired private PasswordEncoder encoder;

    @Test
    void chargeLesAnnoncesFictivesSansDoublonEtLesMarque() throws Exception {
        seeder.run(new DefaultApplicationArguments());
        seeder.run(new DefaultApplicationArguments());
        assertThat(listings.count()).isEqualTo(48);
        assertThat(listings.findAll()).allMatch(listing -> listing.isSample());
        assertThat(encoder.matches("motdepasse", user("employeur@example.org").getPasswordHash())).isTrue();
        mvc.perform(get("/api/listings").param("q", "comptable"))
                .andExpect(jsonPath("$.items[0].isSample").value(true));
    }
}
