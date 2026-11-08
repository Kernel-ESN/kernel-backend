package com.kernelconsulting.plateforme.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Limite les essais de connexion et d'inscription par adresse IP. */
@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter limiter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())
                || !(request.getRequestURI().equals("/api/auth/login")
                        || request.getRequestURI().equals("/api/auth/register"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (limiter.tryAcquire(request.getRemoteAddr())) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":429,\"message\":\"Trop d'essais. Réessayez dans une minute.\"}");
    }
}
