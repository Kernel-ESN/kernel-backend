package com.kernelconsulting.plateforme.application.api.controller;

import com.kernelconsulting.plateforme.application.api.dto.ApplicationResponse;
import com.kernelconsulting.plateforme.application.services.ApplicationService;
import com.kernelconsulting.plateforme.application.services.ApplicationSubmission;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService service;

    @GetMapping("/api/me/applications")
    List<ApplicationResponse> mine() {
        return service.mine();
    }

    @PostMapping("/api/listings/{id}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    ApplicationResponse apply(
            @PathVariable UUID id,
            @RequestPart(name = "cv", required = false) MultipartFile cv,
            @RequestParam(defaultValue = "false") boolean useSavedCv,
            @RequestParam(defaultValue = "false") boolean keepCv,
            @RequestParam(defaultValue = "") String letter,
            @RequestParam(defaultValue = "false") boolean consent) {
        return service.apply(id, new ApplicationSubmission(cv, useSavedCv, keepCv, letter, consent));
    }
}
