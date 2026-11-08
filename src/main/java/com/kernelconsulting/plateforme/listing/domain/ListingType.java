package com.kernelconsulting.plateforme.listing.domain;

import lombok.Getter;

/** Chaque type appartient à un univers, comme dans le cahier des charges §3. */
@Getter
public enum ListingType {
    CDI(Universe.EMPLOI, "cdi"),
    CDD(Universe.EMPLOI, "cdd"),
    CDD_RENOUVELABLE(Universe.EMPLOI, "cdd"),
    INTERIM(Universe.EMPLOI, "interim"),
    STAGE(Universe.EMPLOI, "stage"),
    APPEL_OFFRES(Universe.MARCHE, "ao"),
    AMI(Universe.MARCHE, "ami"),
    CONSULTATION(Universe.MARCHE, "consultation"),
    PRESTATION(Universe.MARCHE, "prestation"),
    FORMATION(Universe.AUTRE, "formation"),
    CONCOURS(Universe.AUTRE, "concours"),
    DIVERS(Universe.AUTRE, "annonce");

    private final Universe universe;
    private final String slugPrefix;

    ListingType(Universe universe, String slugPrefix) {
        this.universe = universe;
        this.slugPrefix = slugPrefix;
    }
}
