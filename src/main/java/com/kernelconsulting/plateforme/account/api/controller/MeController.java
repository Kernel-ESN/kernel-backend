package com.kernelconsulting.plateforme.account.api.controller;

import com.kernelconsulting.plateforme.account.api.dto.CvResponse;
import com.kernelconsulting.plateforme.account.api.dto.ProfileRequest;
import com.kernelconsulting.plateforme.account.api.dto.UserResponse;
import com.kernelconsulting.plateforme.account.services.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final AccountService accounts;

    @GetMapping
    UserResponse me() {
        return accounts.me();
    }

    @PutMapping
    UserResponse update(@Valid @RequestBody ProfileRequest request) {
        return accounts.updateProfile(request);
    }

    @PutMapping("/organization/document")
    UserResponse replaceDocument(@RequestPart("document") MultipartFile document) {
        return accounts.replaceOrganizationDocument(document);
    }

    @GetMapping("/cv")
    ResponseEntity<CvResponse> cv() {
        return accounts.cv().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/cv")
    CvResponse saveCv(@RequestPart("cv") MultipartFile file) {
        return accounts.saveCv(file);
    }

    @DeleteMapping("/cv")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeCv() {
        accounts.removeCv();
    }
}
