package com.kernelconsulting.plateforme.account.services;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/** Ouvre et ferme la session. L'identifiant de session change à la connexion. */
@Service
@RequiredArgsConstructor
public class SessionService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;

    /** @throws org.springframework.security.authentication.BadCredentialsException si le mot de passe est faux */
    public void open(String email, String password, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true);
        request.changeSessionId();
        contextRepository.saveContext(context, request, response);
    }

    /** Ferme la session en cours. */
    public void close(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());
    }
}
