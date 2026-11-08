package com.kernelconsulting.plateforme.alert.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kernelconsulting.plateforme.listing.domain.ListingType;
import com.kernelconsulting.plateforme.listing.domain.Universe;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AlertCriteria(
        Universe universe,
        ListingType type,
        @Size(max = 40) String sectorId,
        @Size(max = 40) String regionId,
        @Size(max = 120) String keywords) {
}
