package com.kernelconsulting.plateforme.moderation.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kernelconsulting.plateforme.account.domain.AppUser;
import com.kernelconsulting.plateforme.account.domain.UserRole;
import com.kernelconsulting.plateforme.account.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    private static final String STRONG = "un-mot-de-passe-long";

    @Mock AppUserRepository users;

    private AdminBootstrap bootstrap(String email, String password) {
        return new AdminBootstrap(users, PasswordEncoderFactories.createDelegatingPasswordEncoder(), email, password);
    }

    @Test
    void neCreeRienSansVariable() {
        bootstrap("", "").run(null);
        verify(users, never()).save(any());
    }

    @Test
    void creeUnModerateurAvecUnMotDePasseChiffre() {
        bootstrap(" Admin@Example.org ", STRONG).run(null);
        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("admin@example.org");
        assertThat(saved.getValue().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(saved.getValue().getPasswordHash()).doesNotContain(STRONG);
    }

    @Test
    void neTouchePasAUnCompteExistant() {
        when(users.existsByEmail("admin@example.org")).thenReturn(true);
        bootstrap("admin@example.org", STRONG).run(null);
        verify(users, never()).save(any());
    }

    @Test
    void refuseDeDemarrerAvecUnMotDePasseCourt() {
        assertThatThrownBy(() -> bootstrap("admin@example.org", "court").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("12 caractères");
        verify(users, never()).save(any());
    }
}
