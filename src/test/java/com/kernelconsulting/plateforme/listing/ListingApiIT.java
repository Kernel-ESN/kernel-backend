package com.kernelconsulting.plateforme.listing;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kernelconsulting.plateforme.account.domain.Organization;
import com.kernelconsulting.plateforme.listing.domain.Listing;
import com.kernelconsulting.plateforme.listing.domain.ListingStatus;
import com.kernelconsulting.plateforme.listing.domain.ListingType;
import com.kernelconsulting.plateforme.support.IntegrationTest;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ListingApiIT extends IntegrationTest {

    @Test
    void renvoieUneFicheConformeAuContrat() throws Exception {
        Listing listing = withBudget(published("Réseau informatique", ListingType.APPEL_OFFRES, "informatique", "niamey"), 45_000_000, false);
        mvc.perform(get("/api/listings/MARCHE/" + listing.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listing.getId().toString()))
                .andExpect(jsonPath("$.universe").value("MARCHE"))
                .andExpect(jsonPath("$.type").value("APPEL_OFFRES"))
                .andExpect(jsonPath("$.description", contains("Premier paragraphe.", "Deuxième paragraphe.")))
                .andExpect(jsonPath("$.organization.verified").value(true))
                .andExpect(jsonPath("$.howToApply.mode").value("EN_LIGNE"))
                .andExpect(jsonPath("$.budget.amount").value(45000000))
                .andExpect(jsonPath("$.budget.hidden").value(false))
                .andExpect(jsonPath("$.status").value("PUBLIEE"))
                .andExpect(jsonPath("$.isSample").value(false))
                .andExpect(jsonPath("$.attachments", hasSize(0)));
    }

    @Test
    void neLaissePasSortirUnBudgetMasque() throws Exception {
        Listing listing = withBudget(published("Marché", ListingType.APPEL_OFFRES, "btp", "niamey"), 99_000_000, true);
        mvc.perform(get("/api/listings/MARCHE/" + listing.getSlug()))
                .andExpect(jsonPath("$.budget.amount").value(0))
                .andExpect(jsonPath("$.budget.hidden").value(true));
    }

    @Test
    void trouveSansAccentNiMajusculeEtAvecLesVariantesFrancaises() throws Exception {
        published("Audit de sécurité des systèmes", ListingType.CONSULTATION, "informatique", "niamey");
        published("Comptables confirmés", ListingType.CDI, "finance", "niamey");
        mvc.perform(get("/api/listings").param("q", "SECURITE"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Audit de sécurité des systèmes"));
        mvc.perform(get("/api/listings").param("q", "comptable"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Comptables confirmés"));
    }

    @Test
    void exigeChaqueMotCherche() throws Exception {
        published("Comptable confirmé", ListingType.CDI, "finance", "niamey");
        mvc.perform(get("/api/listings").param("q", "comptable zzzz")).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void placeUnTitreAvantUneSimpleMention() throws Exception {
        Listing mention = published("Assistant de direction", ListingType.CDI, "administration", "niamey");
        mention.setDescription("Le poste travaille avec un comptable. Paragraphe assez long pour la recherche.");
        listings.save(mention);
        published("Comptable", ListingType.CDI, "finance", "niamey");
        mvc.perform(get("/api/listings").param("q", "comptable").param("sort", "pertinence"))
                .andExpect(jsonPath("$.items[0].title").value("Comptable"))
                .andExpect(jsonPath("$.items[1].title").value("Assistant de direction"));
    }

    @Test
    void filtreParUniversTypeSecteurEtRegion() throws Exception {
        published("Réseau à Niamey", ListingType.APPEL_OFFRES, "informatique", "niamey");
        published("Réseau à Zinder", ListingType.APPEL_OFFRES, "informatique", "zinder");
        published("Forages", ListingType.APPEL_OFFRES, "eau", "niamey");
        published("Développeur", ListingType.CDI, "informatique", "niamey");
        mvc.perform(get("/api/listings").param("universe", "MARCHE").param("sectorId", "informatique").param("regionId", "niamey"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Réseau à Niamey"));
        mvc.perform(get("/api/listings").param("type", "CDI")).andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void retireExpireesEtNonPublieesDeLaListe() throws Exception {
        published("Active", ListingType.CDI, "finance", "niamey");
        Listing expired = published("Expirée", ListingType.CDI, "finance", "niamey");
        expired.setDeadline(Instant.now().minus(Duration.ofDays(1)));
        listings.save(expired);
        Listing pending = published("En attente", ListingType.CDI, "finance", "niamey");
        pending.setStatus(ListingStatus.EN_ATTENTE);
        listings.save(pending);
        mvc.perform(get("/api/listings"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Active"));
    }

    @Test
    void garderUneAnnonceExpireeConsultableAvecSonStatut() throws Exception {
        Listing expired = published("Expirée", ListingType.CDI, "finance", "niamey");
        expired.setDeadline(Instant.now().minus(Duration.ofDays(1)));
        listings.save(expired);
        mvc.perform(get("/api/listings/EMPLOI/" + expired.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIREE"));
    }

    @Test
    void cacheUneAnnonceNonPublieeOuDansUnAutreUnivers() throws Exception {
        Listing pending = published("En attente", ListingType.CDI, "finance", "niamey");
        pending.setStatus(ListingStatus.EN_ATTENTE);
        listings.save(pending);
        mvc.perform(get("/api/listings/EMPLOI/" + pending.getSlug()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cette annonce n'existe pas ou a été retirée."));
        Listing job = published("Poste", ListingType.CDI, "finance", "niamey");
        mvc.perform(get("/api/listings/MARCHE/" + job.getSlug())).andExpect(status().isNotFound());
    }

    @Test
    void trieParDateLimiteEtPagine() throws Exception {
        Listing later = published("Tard", ListingType.CDI, "finance", "niamey");
        Listing sooner = published("Tôt", ListingType.CDI, "finance", "niamey");
        sooner.setDeadline(Instant.now().plus(Duration.ofDays(2)));
        listings.save(sooner);
        mvc.perform(get("/api/listings").param("sort", "expire").param("limit", "1"))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("Tôt"));
        mvc.perform(get("/api/listings").param("sort", "expire").param("limit", "1").param("offset", "1"))
                .andExpect(jsonPath("$.items[0].title").value(later.getTitle()));
    }

    @Test
    void limiteLaFenetreDeDateLimite() throws Exception {
        published("Dans dix jours", ListingType.CDI, "finance", "niamey");
        Listing soon = published("Bientôt", ListingType.CDI, "finance", "niamey");
        soon.setDeadline(Instant.now().plus(Duration.ofDays(3)));
        listings.save(soon);
        mvc.perform(get("/api/listings").param("deadlineWithinDays", "7"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Bientôt"));
    }

    @Test
    void refuseDesCriteresInvalides() throws Exception {
        mvc.perform(get("/api/listings").param("limit", "601")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/listings").param("deadlineWithinDays", "9")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/listings").param("universe", "X"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La requête est incomplète ou mal formée."));
    }

    @Test
    void resisteAUneInjectionDansLaRecherche() throws Exception {
        published("Comptable", ListingType.CDI, "finance", "niamey");
        mvc.perform(get("/api/listings").param("q", "'; DROP TABLE listing; --")).andExpect(status().isOk());
        mvc.perform(get("/api/listings")).andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void compteLesAnnoncesActivesParUnivers() throws Exception {
        published("A", ListingType.CDI, "finance", "niamey");
        published("B", ListingType.APPEL_OFFRES, "finance", "niamey");
        published("C", ListingType.APPEL_OFFRES, "finance", "niamey");
        mvc.perform(get("/api/listings/counts"))
                .andExpect(jsonPath("$.EMPLOI").value(1))
                .andExpect(jsonPath("$.MARCHE").value(2))
                .andExpect(jsonPath("$.AUTRE").value(0));
    }

    @Test
    void proposeDesAnnoncesSimilairesDAbordDuMemeSecteur() throws Exception {
        Listing source = published("Source", ListingType.CDI, "finance", "niamey");
        published("Même région", ListingType.CDI, "sante", "niamey");
        published("Même secteur", ListingType.CDI, "finance", "zinder");
        published("Sans rapport", ListingType.CDI, "sante", "zinder");
        published("Autre univers", ListingType.APPEL_OFFRES, "finance", "niamey");
        mvc.perform(get("/api/listings/" + source.getId() + "/similar"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].title").value("Même secteur"))
                .andExpect(jsonPath("$.items[1].title").value("Même région"));
    }

    @Test
    void cumuleLesValeursDUnMemeFiltre() throws Exception {
        published("Réseau à Niamey", ListingType.CDI, "informatique", "niamey");
        published("Réseau à Zinder", ListingType.CDD, "informatique", "zinder");
        published("Réseau à Dosso", ListingType.STAGE, "informatique", "dosso");
        mvc.perform(get("/api/listings").param("regionId", "niamey", "zinder"))
                .andExpect(jsonPath("$.total").value(2));
        mvc.perform(get("/api/listings").param("type", "CDI", "STAGE").param("regionId", "dosso"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void chaqueFiltreCompteSansSesPropresChoix() throws Exception {
        published("Réseau à Niamey", ListingType.CDI, "informatique", "niamey");
        published("Banque à Niamey", ListingType.CDI, "finance", "niamey");
        published("Réseau à Zinder", ListingType.CDD, "informatique", "zinder");
        Listing expired = published("Expirée", ListingType.CDI, "informatique", "niamey");
        expired.setDeadline(Instant.now().minus(Duration.ofDays(1)));
        listings.save(expired);
        mvc.perform(get("/api/listings/facets").param("regionId", "niamey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.region.niamey").value(2))
                .andExpect(jsonPath("$.region.zinder").value(1))
                .andExpect(jsonPath("$.sector.informatique").value(1))
                .andExpect(jsonPath("$.sector.finance").value(1))
                .andExpect(jsonPath("$.type.CDI").value(2))
                .andExpect(jsonPath("$.type.CDD").doesNotExist())
                .andExpect(jsonPath("$.organizationType.ONG").value(2))
                .andExpect(jsonPath("$.within.7").value(0))
                .andExpect(jsonPath("$.within.30").value(2))
                .andExpect(jsonPath("$.since.1").value(0))
                .andExpect(jsonPath("$.since.7").value(2));
    }

    @Test
    void filtreParTypeDOrganisationEtDateDePublication() throws Exception {
        published("Récente", ListingType.CDI, "informatique", "niamey");
        Listing old = published("Ancienne", ListingType.CDI, "informatique", "niamey");
        old.setPublishedAt(Instant.now().minus(Duration.ofDays(20)));
        listings.save(old);
        mvc.perform(get("/api/listings").param("publishedWithinDays", "7")).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/listings").param("organizationType", "ONG")).andExpect(jsonPath("$.total").value(2));
        mvc.perform(get("/api/listings").param("organizationType", "MINISTERE")).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/listings").param("publishedWithinDays", "2")).andExpect(status().isBadRequest());
    }

    @Test
    void presenteLesOrganisationsSansExposerLeursContacts() throws Exception {
        Organization active = organization("Ministère de l'Éducation");
        active.setContactPhone("+227 90 00 00 00");
        organizations.save(active);
        for (String title : new String[] {"Premier poste", "Second poste"}) {
            Listing listing = published(title, ListingType.CDI, "education", "niamey");
            listing.setOrganization(active);
            listings.save(listing);
        }
        Organization closed = organization("Organisation sans annonce ouverte");
        Listing expired = published("Ancien marché", ListingType.CONSULTATION, "btp", "zinder");
        expired.setOrganization(closed);
        expired.setDeadline(Instant.now().minus(Duration.ofDays(1)));
        listings.save(expired);
        Organization silent = organization("Organisation jamais publiée");

        mvc.perform(get("/api/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ministère de l'Éducation"))
                .andExpect(jsonPath("$[0].slug").value(active.getSlug()))
                .andExpect(jsonPath("$[0].openListings").value(2))
                .andExpect(jsonPath("$[0].verified").value(true))
                .andExpect(jsonPath("$[0].contactPhone").doesNotExist())
                .andExpect(jsonPath("$[?(@.name == 'Organisation sans annonce ouverte')]", hasSize(0)));
        mvc.perform(get("/api/organizations/" + active.getSlug())).andExpect(jsonPath("$.openListings").value(2));
        mvc.perform(get("/api/organizations/" + closed.getSlug())).andExpect(jsonPath("$.openListings").value(0));
        mvc.perform(get("/api/organizations/" + silent.getSlug())).andExpect(status().isNotFound());
        mvc.perform(get("/api/listings").param("organizationId", active.getId().toString()))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].organization.slug").value(active.getSlug()));
        org.junit.jupiter.api.Assertions.assertTrue(active.getSlug().startsWith("ministere-de-l-education-"));
    }
}
