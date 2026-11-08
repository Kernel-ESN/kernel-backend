package com.kernelconsulting.plateforme.listing.domain;

import com.kernelconsulting.plateforme.account.domain.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "saved_listing")
@Getter
@NoArgsConstructor
public class SavedListing {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public SavedListing(AppUser user, Listing listing) {
        this.user = user;
        this.listing = listing;
    }
}
