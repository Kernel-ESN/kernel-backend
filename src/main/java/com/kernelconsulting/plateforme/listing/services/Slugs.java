package com.kernelconsulting.plateforme.listing.services;

import java.text.Normalizer;
import java.util.Locale;

/** Adresses lisibles, comme {@code ao-reseau-informatique-niamey-3f9a}. */
public final class Slugs {

    private Slugs() {
    }

    public static String slugify(String text) {
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    /** Compose l'adresse et la limite à 180 caractères avant son suffixe unique. */
    public static String of(String prefix, String title, String city, String suffix) {
        String base = slugify(prefix + " " + title + " " + city);
        String cut = base.length() > 180 ? base.substring(0, 180).replaceAll("-$", "") : base;
        return cut + "-" + suffix;
    }
}
