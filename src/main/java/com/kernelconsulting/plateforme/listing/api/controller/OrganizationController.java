package com.kernelconsulting.plateforme.listing.api.controller;

import com.kernelconsulting.plateforme.listing.api.dto.PublicOrganizationResponse;
import com.kernelconsulting.plateforme.listing.services.ListingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final ListingService service;

    @GetMapping
    List<PublicOrganizationResponse> list() {
        return service.organizations();
    }

    @GetMapping("/{slug}")
    PublicOrganizationResponse get(@PathVariable String slug) {
        return service.organization(slug);
    }
}
