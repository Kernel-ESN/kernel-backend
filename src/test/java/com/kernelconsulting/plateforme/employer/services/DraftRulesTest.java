package com.kernelconsulting.plateforme.employer.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.kernelconsulting.plateforme.employer.api.dto.ListingDraftRequest;
import com.kernelconsulting.plateforme.listing.domain.ApplyMode;
import com.kernelconsulting.plateforme.listing.domain.ListingType;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DraftRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    private static ListingDraftRequest draft(String deadline, ApplyMode mode, String email, String url) {
        return new ListingDraftRequest(
                ListingType.CDI, "Comptable confirmé", "Nous cherchons un comptable pour un projet.",
                "Description assez longue pour respecter la règle des cinquante caractères.",
                "finance", "niamey", "Niamey", deadline, mode, email, url, "Postulez.", null);
    }

    @Test
    void accepteUneAnnonceComplete() {
        assertThat(DraftRules.problems(draft("2026-12-01", ApplyMode.EN_LIGNE, "", ""), TODAY)).isEmpty();
    }

    @Test
    void accepteUneDateLimiteAujourdhuiMaisPasHier() {
        assertThat(DraftRules.problems(draft("2026-11-06", ApplyMode.EN_LIGNE, "", ""), TODAY)).isEmpty();
        assertThat(DraftRules.problems(draft("2026-11-05", ApplyMode.EN_LIGNE, "", ""), TODAY))
                .containsExactly("Choisissez une date limite à venir.");
    }

    @Test
    void refuseUneDateMalEcrite() {
        assertThat(DraftRules.problems(draft("demain", ApplyMode.EN_LIGNE, "", ""), TODAY))
                .containsExactly("Choisissez une date limite à venir.");
    }

    @Test
    void exigeLeContactDuModeChoisi() {
        assertThat(DraftRules.problems(draft("2026-12-01", ApplyMode.EMAIL, "pas-une-adresse", ""), TODAY))
                .containsExactly("Indiquez une adresse e-mail valide.");
        assertThat(DraftRules.problems(draft("2026-12-01", ApplyMode.LIEN, "", "http://site.test"), TODAY))
                .containsExactly("Indiquez une adresse commençant par https://");
        assertThat(DraftRules.problems(draft("2026-12-01", ApplyMode.LIEN, "", "https://site.test"), TODAY)).isEmpty();
    }
}
