package com.kernelconsulting.plateforme.account.domain;

import com.kernelconsulting.plateforme.listing.services.Slugs;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "organization")
@Getter
@Setter
@NoArgsConstructor
public class Organization {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    /** Fixée au premier nom : un lien partagé reste valable si l'organisation est renommée. */
    @Column(nullable = false, updatable = false)
    @Setter(lombok.AccessLevel.NONE)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Verification verification = Verification.EN_ATTENTE;

    private String contactPhone;
    private String documentPath;
    private String documentName;
    private String verificationNote;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public void setName(String name) {
        this.name = name;
        if (slug == null) {
            String base = Slugs.slugify(name);
            String cut = base.length() > 180 ? base.substring(0, 180).replaceAll("-$", "") : base;
            slug = cut + "-" + id.toString().substring(0, 8);
        }
    }

    public boolean isVerified() {
        return verification == Verification.VERIFIEE;
    }
}
