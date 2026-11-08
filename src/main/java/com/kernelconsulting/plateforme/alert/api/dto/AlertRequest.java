package com.kernelconsulting.plateforme.alert.api.dto;

import com.kernelconsulting.plateforme.alert.domain.AlertChannel;
import com.kernelconsulting.plateforme.alert.domain.AlertFrequency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AlertRequest(
        @NotNull @Valid AlertCriteria criteria,
        @NotNull AlertChannel channel,
        @NotNull AlertFrequency frequency) {
}
