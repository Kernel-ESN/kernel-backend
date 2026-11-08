package com.kernelconsulting.plateforme.account.services;

import com.kernelconsulting.plateforme.account.repository.AppUserRepository;
import com.kernelconsulting.plateforme.common.security.AppPrincipal;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Charge un compte pour Spring Security. */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return users.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .map(user -> new AppPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRole().name()))
                .orElseThrow(() -> new UsernameNotFoundException("Compte inconnu"));
    }
}
