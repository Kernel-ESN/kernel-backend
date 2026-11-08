package com.kernelconsulting.plateforme.common.security;

import java.util.UUID;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Accès à l'utilisateur de la session. */
@Component
public class CurrentUser {

    /**
     * @return l'identifiant de l'utilisateur connecté
     * @throws AuthenticationCredentialsNotFoundException si personne n'est connecté
     */
    public UUID id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppPrincipal principal) {
            return principal.id();
        }
        throw new AuthenticationCredentialsNotFoundException("Aucune session");
    }
}
