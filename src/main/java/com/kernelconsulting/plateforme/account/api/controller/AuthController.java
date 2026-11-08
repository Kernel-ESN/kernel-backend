package com.kernelconsulting.plateforme.account.api.controller;

import com.kernelconsulting.plateforme.account.api.dto.LoginRequest;
import com.kernelconsulting.plateforme.account.api.dto.RegisterRequest;
import com.kernelconsulting.plateforme.account.api.dto.UserResponse;
import com.kernelconsulting.plateforme.account.services.AccountService;
import com.kernelconsulting.plateforme.account.services.RegistrationParser;
import com.kernelconsulting.plateforme.account.services.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AccountService accounts;
    private final SessionService sessions;
    private final RegistrationParser parser;

    /** Dépose le cookie CSRF avant le premier envoi d'une application monopage. */
    @GetMapping("/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void csrf(CsrfToken token) {
        token.getToken();
    }

    @PostMapping("/login")
    UserResponse login(
            @Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        sessions.open(body.email(), body.password(), request, response);
        return accounts.me();
    }

    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(
            @Valid @RequestBody RegisterRequest body, HttpServletRequest request, HttpServletResponse response) {
        return registerAndOpen(body, null, request, response);
    }

    @PostMapping(path = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse registerWithDocument(
            @RequestParam("data") String data,
            @RequestPart("document") MultipartFile document,
            HttpServletRequest request,
            HttpServletResponse response) {
        return registerAndOpen(parser.parse(data), document, request, response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request, HttpServletResponse response) {
        sessions.close(request, response);
    }

    private UserResponse registerAndOpen(
            RegisterRequest body, MultipartFile document, HttpServletRequest request, HttpServletResponse response) {
        accounts.register(body, document);
        sessions.open(body.email(), body.password(), request, response);
        return accounts.me();
    }
}
