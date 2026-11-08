package com.kernelconsulting.plateforme.listing.services;

import com.kernelconsulting.plateforme.account.repository.AppUserRepository;
import com.kernelconsulting.plateforme.common.exception.ApiException;
import com.kernelconsulting.plateforme.common.security.CurrentUser;
import com.kernelconsulting.plateforme.listing.api.dto.SavedListingResponse;
import com.kernelconsulting.plateforme.listing.api.mapper.ListingMapper;
import com.kernelconsulting.plateforme.listing.domain.SavedListing;
import com.kernelconsulting.plateforme.listing.repository.ListingRepository;
import com.kernelconsulting.plateforme.listing.repository.SavedListingRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Annonces enregistrées de l'utilisateur connecté. */
@Service
@RequiredArgsConstructor
public class SavedListingService {

    private final SavedListingRepository saved;
    private final ListingRepository listings;
    private final AppUserRepository users;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<SavedListingResponse> list() {
        return saved.findWithListing(currentUser.id()).stream()
                .map(item -> ListingMapper.toSaved(item.getListing()))
                .toList();
    }

    /** Enregistre une annonce. Enregistrer deux fois ne change rien. */
    @Transactional
    public void add(UUID listingId) {
        UUID userId = currentUser.id();
        if (saved.existsByUserIdAndListingId(userId, listingId)) {
            return;
        }
        var listing = listings.findById(listingId)
                .orElseThrow(() -> ApiException.notFound("Cette annonce n'existe pas ou a été retirée."));
        saved.save(new SavedListing(users.getReferenceById(userId), listing));
    }

    @Transactional
    public void remove(UUID listingId) {
        saved.deleteByUserIdAndListingId(currentUser.id(), listingId);
    }
}
