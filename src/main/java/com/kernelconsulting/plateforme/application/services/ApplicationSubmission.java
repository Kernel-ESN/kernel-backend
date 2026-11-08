package com.kernelconsulting.plateforme.application.services;

import org.springframework.web.multipart.MultipartFile;

/** Contenu d'une candidature : soit un fichier joint, soit le CV enregistré du compte. */
public record ApplicationSubmission(MultipartFile cv, boolean useSavedCv, boolean keepCv, String letter, boolean consent) {
}
