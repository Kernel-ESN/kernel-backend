package com.kernelconsulting.plateforme.listing.api.dto;

import java.util.List;

public record ListingPageResponse(List<ListingResponse> items, long total) {
}
