package com.kernelconsulting.plateforme.alert.domain;

import com.kernelconsulting.plateforme.account.domain.AppUser;
import com.kernelconsulting.plateforme.listing.domain.ListingType;
import com.kernelconsulting.plateforme.listing.domain.Universe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "alert")
@Getter
@Setter
@NoArgsConstructor
public class Alert {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    private Universe universe;

    @Enumerated(EnumType.STRING)
    private ListingType type;

    private String sectorId;
    private String regionId;
    private String keywords;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertFrequency frequency;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
