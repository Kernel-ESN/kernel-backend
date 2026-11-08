package com.kernelconsulting.plateforme.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalFileStorageTest {

    @TempDir Path root;

    @Test
    void neReprendJamaisLeNomDOrigineDansLeChemin() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(root.toString());
        String reference = storage.store(new MockMultipartFile("f", "../../etc/passwd", "text/plain", "x".getBytes()), "cv");
        assertThat(reference).startsWith("cv/").doesNotContain("passwd");
        assertThat(Files.readString(root.resolve(reference))).isEqualTo("x");
    }

    @Test
    void supprimeSansErreurMemeSiRienNExiste() {
        LocalFileStorage storage = new LocalFileStorage(root.toString());
        String reference = storage.store(new MockMultipartFile("f", "a", "text/plain", "x".getBytes()), "cv");
        storage.delete(reference);
        assertThat(root.resolve(reference)).doesNotExist();
        storage.delete(reference);
        storage.delete(null);
    }

    @Test
    void refuseDeSortirDuDossier() {
        LocalFileStorage storage = new LocalFileStorage(root.toString());
        assertThatThrownBy(() -> storage.delete("../hors-du-dossier")).isInstanceOf(IllegalArgumentException.class);
    }
}
