package com.kernelconsulting.plateforme.common.storage;

import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/** Stockage des fichiers privés. Interface pour passer plus tard à un stockage compatible S3. */
public interface FileStorage {

    /** @return la référence interne du fichier, jamais exposée par l'API */
    String store(MultipartFile file, String folder);

    /** @return le contenu du fichier, vide si la référence est inconnue */
    Optional<Resource> read(String reference);

    /** Supprime un fichier. Sans effet si la référence est nulle ou inconnue. */
    void delete(String reference);
}
