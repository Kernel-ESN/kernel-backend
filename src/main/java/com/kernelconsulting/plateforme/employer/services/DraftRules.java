package com.kernelconsulting.plateforme.employer.services;

import com.kernelconsulting.plateforme.employer.api.dto.ListingDraftRequest;
import com.kernelconsulting.plateforme.listing.domain.ApplyMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Règles qu'une annonce doit respecter pour partir en validation. */
public final class DraftRules {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern HTTPS_URL = Pattern.compile("^https://\\S+$");

    private DraftRules() {
    }

    /** @return la liste des problèmes, vide si l'annonce peut être soumise */
    public static List<String> problems(ListingDraftRequest draft, LocalDate today) {
        List<String> problems = new ArrayList<>();
        length(problems, draft.title(), 5, 140, "L'intitulé doit compter de 5 à 140 caractères.");
        length(problems, draft.summary(), 20, 300, "Le résumé doit compter de 20 à 300 caractères.");
        length(problems, draft.description(), 50, 10000, "La description doit compter au moins 50 caractères.");
        length(problems, draft.city(), 1, 80, "Indiquez la ville.");
        length(problems, draft.instructions(), 1, 1000, "Indiquez les consignes.");
        if (isBlank(draft.sectorId())) {
            problems.add("Choisissez un secteur.");
        }
        if (isBlank(draft.regionId())) {
            problems.add("Choisissez une région.");
        }
        LocalDate deadline = parseDate(draft.deadline());
        if (deadline == null || !deadline.isAfter(today.minusDays(1))) {
            problems.add("Choisissez une date limite à venir.");
        }
        if (draft.applyMode() == ApplyMode.EMAIL && (draft.applyEmail() == null || !EMAIL.matcher(draft.applyEmail()).matches())) {
            problems.add("Indiquez une adresse e-mail valide.");
        }
        if (draft.applyMode() == ApplyMode.LIEN && (draft.applyUrl() == null || !HTTPS_URL.matcher(draft.applyUrl()).matches())) {
            problems.add("Indiquez une adresse commençant par https://");
        }
        return problems;
    }

    /** @return la date, ou null si elle est absente ou mal écrite */
    public static LocalDate parseDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static void length(List<String> problems, String value, int min, int max, String message) {
        int length = value == null ? 0 : value.trim().length();
        if (length < min || length > max) {
            problems.add(message);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
