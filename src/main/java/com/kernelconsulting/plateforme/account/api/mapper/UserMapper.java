package com.kernelconsulting.plateforme.account.api.mapper;

import com.kernelconsulting.plateforme.account.api.dto.CvResponse;
import com.kernelconsulting.plateforme.account.api.dto.UserResponse;
import com.kernelconsulting.plateforme.account.domain.AppUser;
import com.kernelconsulting.plateforme.account.domain.Organization;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(AppUser user) {
        Organization organization = user.getOrganization();
        UserResponse.OrganizationResponse part = organization == null
                ? null
                : new UserResponse.OrganizationResponse(
                        organization.getName(),
                        organization.getType().name(),
                        organization.getVerification().name(),
                        organization.getDocumentName(),
                        organization.getVerificationNote());
        return new UserResponse(
                user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole().name(), part);
    }

    public static CvResponse toCv(AppUser user) {
        return new CvResponse(user.getCvName(), user.getCvSize(), user.getCvUpdatedAt());
    }
}
