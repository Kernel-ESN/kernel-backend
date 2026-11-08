package com.kernelconsulting.plateforme.account.services;

import com.kernelconsulting.plateforme.account.api.dto.RegisterRequest;
import com.kernelconsulting.plateforme.common.exception.ApiException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Lit la partie {@code data} d'une inscription multipart et applique les mêmes règles que le JSON. */
@Component
@RequiredArgsConstructor
public class RegistrationParser {

    private final ObjectMapper mapper;
    private final Validator validator;

    /** @throws ApiException 400 si le contenu est illisible ou invalide */
    public RegisterRequest parse(String data) {
        RegisterRequest request;
        try {
            request = mapper.readValue(data, RegisterRequest.class);
        } catch (JacksonException e) {
            throw ApiException.badRequest("La requête est incomplète ou mal formée.", List.of());
        }
        List<String> details = validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(path -> path + " : invalide")
                .toList();
        if (!details.isEmpty()) {
            throw ApiException.badRequest("Certains champs ne sont pas valides.", details);
        }
        return request;
    }
}
