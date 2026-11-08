package com.kernelconsulting.plateforme.listing.api.mapper;

import com.kernelconsulting.plateforme.account.domain.Organization;
import com.kernelconsulting.plateforme.listing.api.dto.ListingResponse;
import com.kernelconsulting.plateforme.listing.api.dto.SavedListingResponse;
import com.kernelconsulting.plateforme.listing.domain.Listing;
import com.kernelconsulting.plateforme.listing.domain.ListingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public final class ListingMapper {

    private ListingMapper() {
    }

    public static ListingResponse toResponse(Listing listing, Instant now) {
        Organization organization = listing.getOrganization();
        return new ListingResponse(
                listing.getId(),
                listing.getSlug(),
                listing.getUniverse().name(),
                listing.getType().name(),
                listing.getReference(),
                listing.getTitle(),
                listing.getSummary(),
                paragraphs(listing.getDescription()),
                new ListingResponse.OrganizationPart(
                        organization.getId(), organization.getSlug(), organization.getName(), organization.getType().name(), organization.isVerified()),
                listing.getSectorId(),
                listing.getRegionId(),
                listing.getCity(),
                listing.getCountry(),
                listing.getEducationLevel(),
                listing.getExperienceLevel(),
                listing.getPublishedAt(),
                listing.getDeadline(),
                publicStatus(listing, now).name(),
                new ListingResponse.HowToApply(
                        listing.getApplyMode().name(), listing.getApplyEmail(), listing.getApplyUrl(), listing.getApplyInstructions()),
                budget(listing),
                listing.getAttachments().stream()
                        .map(file -> new ListingResponse.Attachment(file.getName(), file.getUrl(), file.getSizeBytes()))
                        .toList(),
                listing.isSample());
    }

    public static SavedListingResponse toSaved(Listing listing) {
        return new SavedListingResponse(
                listing.getId(), listing.getSlug(), listing.getUniverse().name(), listing.getTitle(), listing.getOrganization().getName());
    }

    /** Une annonce publiée dont la date limite est passée se présente comme expirée. */
    public static ListingStatus publicStatus(Listing listing, Instant now) {
        return listing.getStatus() == ListingStatus.PUBLIEE && listing.isExpiredAt(now)
                ? ListingStatus.EXPIREE
                : listing.getStatus();
    }

    /** Un budget masqué ne sort jamais de l'API, même dans la réponse brute. */
    private static ListingResponse.Budget budget(Listing listing) {
        if (listing.getBudgetAmount() == null) {
            return null;
        }
        BigDecimal amount = listing.isBudgetHidden() ? BigDecimal.ZERO : listing.getBudgetAmount();
        return new ListingResponse.Budget(amount, listing.getBudgetCurrency(), listing.isBudgetHidden());
    }

    private static List<String> paragraphs(String description) {
        return Arrays.stream(description.split("\\R\\s*\\R")).map(String::trim).filter(text -> !text.isEmpty()).toList();
    }
}
