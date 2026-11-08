package com.kernelconsulting.plateforme.common.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Fichiers sur disque, hors de tout dossier servi. Le nom d'origine n'entre jamais dans le chemin. */
@Component
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(@Value("${app.storage.root}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file, String folder) {
        String reference = folder + "/" + UUID.randomUUID();
        Path target = resolve(reference);
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target);
            return reference;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<Resource> read(String reference) {
        if (reference == null) {
            return Optional.empty();
        }
        Path path = resolve(reference);
        return Files.isRegularFile(path) ? Optional.of(new FileSystemResource(path)) : Optional.empty();
    }

    @Override
    public void delete(String reference) {
        if (reference == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(reference));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path resolve(String reference) {
        Path path = root.resolve(reference).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Référence de fichier invalide");
        }
        return path;
    }
}
