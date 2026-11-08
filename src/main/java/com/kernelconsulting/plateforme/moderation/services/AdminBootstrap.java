package com.kernelconsulting.plateforme.moderation.services;

import com.kernelconsulting.plateforme.account.domain.AppUser;
import com.kernelconsulting.plateforme.account.domain.UserRole;
import com.kernelconsulting.plateforme.account.repository.AppUserRepository;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crée le premier modérateur à partir de {@code ADMIN_EMAIL} et {@code ADMIN_PASSWORD}. L'inscription
 * publique ne donne jamais ce rôle. Sans ces variables, rien n'est créé. Un compte existant n'est pas modifié.
 */
@Slf4j
@Component
public class AdminBootstrap implements ApplicationRunner {

    static final int MIN_PASSWORD = 12;

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public AdminBootstrap(
            AppUserRepository users,
            PasswordEncoder encoder,
            @Value("${app.admin.email:}") String email,
            @Value("${app.admin.password:}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank()) {
            return;
        }
        if (password.length() < MIN_PASSWORD) {
            throw new IllegalStateException("ADMIN_PASSWORD doit compter au moins " + MIN_PASSWORD + " caractères.");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(normalized)) {
            return;
        }
        AppUser admin = new AppUser();
        admin.setEmail(normalized);
        admin.setName("Modération");
        admin.setRole(UserRole.ADMIN);
        admin.setPasswordHash(encoder.encode(password));
        users.save(admin);
        log.info("Compte de modération créé.");
    }
}
