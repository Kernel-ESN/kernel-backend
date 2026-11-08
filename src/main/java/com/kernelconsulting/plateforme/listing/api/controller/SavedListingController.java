package com.kernelconsulting.plateforme.listing.api.controller;

import com.kernelconsulting.plateforme.listing.api.dto.SavedListingResponse;
import com.kernelconsulting.plateforme.listing.services.SavedListingService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/saved")
@RequiredArgsConstructor
public class SavedListingController {

    private final SavedListingService service;

    @GetMapping
    List<SavedListingResponse> list() {
        return service.list();
    }

    @PutMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void add(@PathVariable UUID listingId) {
        service.add(listingId);
    }

    @DeleteMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable UUID listingId) {
        service.remove(listingId);
    }
}
