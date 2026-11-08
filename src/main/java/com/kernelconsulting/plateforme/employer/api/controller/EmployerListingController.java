package com.kernelconsulting.plateforme.employer.api.controller;

import com.kernelconsulting.plateforme.employer.api.dto.EmployerListingResponse;
import com.kernelconsulting.plateforme.employer.api.dto.ListingDraftRequest;
import com.kernelconsulting.plateforme.employer.services.EmployerListingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employer/listings")
@RequiredArgsConstructor
public class EmployerListingController {

    private final EmployerListingService service;

    @GetMapping
    List<EmployerListingResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    EmployerListingResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    EmployerListingResponse create(
            @Valid @RequestBody ListingDraftRequest draft, @RequestParam(defaultValue = "false") boolean submit) {
        return service.create(draft, submit);
    }

    @PutMapping("/{id}")
    EmployerListingResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ListingDraftRequest draft,
            @RequestParam(defaultValue = "false") boolean submit) {
        return service.update(id, draft, submit);
    }

    @PostMapping("/{id}/close")
    EmployerListingResponse close(@PathVariable UUID id) {
        return service.close(id);
    }

    @PostMapping("/{id}/duplicate")
    EmployerListingResponse duplicate(@PathVariable UUID id) {
        return service.duplicate(id);
    }

    @PostMapping("/{id}/republish")
    EmployerListingResponse republish(@PathVariable UUID id) {
        return service.republish(id);
    }
}
