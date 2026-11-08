package com.kernelconsulting.plateforme.alert.services;

import com.kernelconsulting.plateforme.account.domain.AppUser;
import com.kernelconsulting.plateforme.account.repository.AppUserRepository;
import com.kernelconsulting.plateforme.alert.api.dto.AlertCriteria;
import com.kernelconsulting.plateforme.alert.api.dto.AlertRequest;
import com.kernelconsulting.plateforme.alert.api.dto.AlertResponse;
import com.kernelconsulting.plateforme.alert.domain.Alert;
import com.kernelconsulting.plateforme.alert.domain.AlertChannel;
import com.kernelconsulting.plateforme.alert.repository.AlertRepository;
import com.kernelconsulting.plateforme.common.exception.ApiException;
import com.kernelconsulting.plateforme.common.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alertes de l'utilisateur connecté. Elles partent vers l'e-mail du compte : personne ne peut abonner
 * l'adresse d'un tiers. L'envoi réel arrive en phase 4.
 */
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alerts;
    private final AppUserRepository users;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<AlertResponse> list() {
        return alerts.findByUserIdOrderByCreatedAtDesc(currentUser.id()).stream().map(AlertService::toResponse).toList();
    }

    /** @throws ApiException 422 pour WhatsApp, pas encore disponible */
    @Transactional
    public AlertResponse create(AlertRequest request) {
        if (request.channel() == AlertChannel.WHATSAPP) {
            throw ApiException.unprocessable("Les alertes par WhatsApp ne sont pas encore disponibles.");
        }
        AppUser user = users.findById(currentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Connectez-vous pour continuer."));
        AlertCriteria criteria = request.criteria();
        Alert alert = new Alert();
        alert.setUser(user);
        alert.setUniverse(criteria.universe());
        alert.setType(criteria.type());
        alert.setSectorId(criteria.sectorId());
        alert.setRegionId(criteria.regionId());
        alert.setKeywords(criteria.keywords());
        alert.setChannel(request.channel());
        alert.setFrequency(request.frequency());
        return toResponse(alerts.save(alert));
    }

    /** @throws ApiException 404 si l'alerte n'existe pas ou appartient à quelqu'un d'autre */
    @Transactional
    public void remove(UUID id) {
        Alert alert = alerts.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> ApiException.notFound("Cette alerte n'existe pas."));
        alerts.delete(alert);
    }

    private static AlertResponse toResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                new AlertCriteria(alert.getUniverse(), alert.getType(), alert.getSectorId(), alert.getRegionId(), alert.getKeywords()),
                alert.getUser().getEmail(),
                alert.getChannel().name(),
                alert.getFrequency().name(),
                alert.getCreatedAt());
    }
}
