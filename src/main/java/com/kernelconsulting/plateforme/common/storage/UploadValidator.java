package com.kernelconsulting.plateforme.common.storage;

import com.kernelconsulting.plateforme.common.exception.ApiException;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contrôle côté serveur : présence, taille et vrai format. L'analyse antivirus exigée par le cahier
 * des charges reste à brancher avant la mise en ligne (voir ADR 0006).
 */
@Component
public class UploadValidator {

    static final long MAX_BYTES = 5_000_000;

    /** @throws ApiException 422 si le fichier est refusé */
    public void check(MultipartFile file, FileKind kind) {
        if (file == null || file.isEmpty()) {
            throw ApiException.unprocessable("Joignez un fichier.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw ApiException.unprocessable("Ce fichier dépasse 5 Mo.");
        }
        byte[] head = new byte[8];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            throw ApiException.unprocessable("Ce fichier n'a pas pu être lu.");
        }
        int length = read;
        boolean accepted = kind.getSignatures().stream().anyMatch(signature -> signature.matches(head, length));
        if (!accepted) {
            throw ApiException.unprocessable("Ce format n'est pas accepté. " + kind.getAdvice());
        }
    }
}
