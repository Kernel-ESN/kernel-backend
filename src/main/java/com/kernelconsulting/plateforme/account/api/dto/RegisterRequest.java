package com.kernelconsulting.plateforme.account.api.dto;

import com.kernelconsulting.plateforme.account.domain.OrganizationType;
import com.kernelconsulting.plateforme.account.domain.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotNull UserRole role,
        @Pattern(regexp = ProfileRequest.PHONE) String phone,
        @Valid OrganizationPart organization) {

    public record OrganizationPart(
            @NotBlank @Size(max = 160) String name,
            @NotNull OrganizationType type) {
    }
}
