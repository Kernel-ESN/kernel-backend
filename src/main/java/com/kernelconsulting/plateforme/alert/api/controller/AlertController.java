package com.kernelconsulting.plateforme.alert.api.controller;

import com.kernelconsulting.plateforme.alert.api.dto.AlertRequest;
import com.kernelconsulting.plateforme.alert.api.dto.AlertResponse;
import com.kernelconsulting.plateforme.alert.services.AlertService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService service;

    @GetMapping
    List<AlertResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AlertResponse create(@Valid @RequestBody AlertRequest request) {
        return service.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable UUID id) {
        service.remove(id);
    }
}
