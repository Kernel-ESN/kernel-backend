package com.kernelconsulting.plateforme.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Session par cookie et protection CSRF pour une application monopage : Angular lit le cookie
 * {@code XSRF-TOKEN} et renvoie {@code X-XSRF-TOKEN}. Voir {@code docs/adr/0006-backend.md}.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain api(HttpSecurity http, AuthRateLimitFilter rateLimit) throws Exception {
        return http
                .csrf(csrf -> csrf.spa())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .securityContext(context -> context.securityContextRepository(securityContextRepository()))
                .addFilterBefore(rateLimit, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/listings/**", "/api/organizations/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/employer/**", "/api/me/organization/**").hasRole("EMPLOYEUR")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, e) -> write(request, response, 401, "Connectez-vous pour continuer."))
                        .accessDeniedHandler((request, response, e) -> write(request, response, 403, "Vous n'avez pas accès à cette ressource.")))
                .build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    private static void write(HttpServletRequest request, HttpServletResponse response, int status, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"status\":%d,\"message\":\"%s\",\"path\":\"%s\"}"
                        .formatted(status, message, request.getRequestURI().replace("\"", "")));
    }
}
