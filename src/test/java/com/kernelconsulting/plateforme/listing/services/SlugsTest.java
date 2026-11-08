package com.kernelconsulting.plateforme.listing.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlugsTest {

    @Test
    void ecritUneAdresseLisibleSansAccent() {
        assertThat(Slugs.of("ao", "Réseau d'été : Tillabéri !", "Niamey", "3f9a"))
                .isEqualTo("ao-reseau-d-ete-tillaberi-niamey-3f9a");
    }

    @Test
    void limiteLaLongueurSansToucherAuSuffixe() {
        String slug = Slugs.of("ao", "x".repeat(300), "Niamey", "3f9a");
        assertThat(slug).hasSizeLessThanOrEqualTo(185).endsWith("-3f9a");
    }
}
