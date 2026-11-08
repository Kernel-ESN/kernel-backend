package com.kernelconsulting.plateforme.account.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileRequest(
        @NotBlank @Size(max = 120) String name,
        @Pattern(regexp = ProfileRequest.PHONE_OR_EMPTY) String phone) {

    static final String PHONE = "^\\+?[0-9 ().-]{8,20}$";
    static final String PHONE_OR_EMPTY = "^$|" + PHONE;
}
