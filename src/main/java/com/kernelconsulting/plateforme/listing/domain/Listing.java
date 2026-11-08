package com.kernelconsulting.plateforme.listing.domain;

import com.kernelconsulting.plateforme.account.domain.AppUser;
import com.kernelconsulting.plateforme.account.domain.Organization;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "listing")
@Getter
@Setter
@NoArgsConstructor
public class Listing {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Universe universe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingType type;

    private String reference;

    @Column(nullable = false)
    private String title = "";

    @Column(nullable = false)
    private String summary = "";

    @Column(nullable = false)
    private String description = "";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    private String sectorId;
    private String regionId;

    @Column(nullable = false)
    private String city = "";

    @Column(nullable = false)
    private String country = "NE";

    private String educationLevel;
    private String experienceLevel;
    private Instant publishedAt;
    private Instant deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatus status = ListingStatus.BROUILLON;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplyMode applyMode = ApplyMode.EN_LIGNE;

    private String applyEmail;
    private String applyUrl;

    @Column(nullable = false)
    private String applyInstructions = "";

    private String moderationNote;

    private BigDecimal budgetAmount;
    private String budgetCurrency;

    @Column(nullable = false)
    private boolean budgetHidden;

    @Column(name = "is_sample", nullable = false)
    private boolean sample;

    @Column(nullable = false)
    private long viewCount;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @ElementCollection
    @CollectionTable(name = "listing_attachment", joinColumns = @JoinColumn(name = "listing_id"))
    @OrderColumn(name = "position")
    private List<ListingAttachment> attachments = new ArrayList<>();

    /** @return vrai si la date limite est passée */
    public boolean isExpiredAt(Instant now) {
        return deadline != null && deadline.isBefore(now);
    }
}
