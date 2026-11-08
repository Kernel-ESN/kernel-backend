package com.kernelconsulting.plateforme.account.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(UUID id, String name, String email, String phone, String role, OrganizationResponse organization) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OrganizationResponse(
            String name, String type, String verification, String documentName, String verificationNote) {
    }
}
