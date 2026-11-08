package com.kernelconsulting.plateforme.common.storage;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kernelconsulting.plateforme.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class UploadValidatorTest {

    private final UploadValidator validator = new UploadValidator();

    private static MockMultipartFile file(byte[] content) {
        return new MockMultipartFile("f", "nom.pdf", "application/pdf", content);
    }

    @Test
    void accepteLesFormatsAttendusSelonLeurContenu() {
        assertThatCode(() -> validator.check(file("%PDF-1.7".getBytes()), FileKind.CV)).doesNotThrowAnyException();
        assertThatCode(() -> validator.check(file(new byte[] {0x50, 0x4B, 0x03, 0x04, 0}), FileKind.CV)).doesNotThrowAnyException();
        assertThatCode(() -> validator.check(file(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}), FileKind.ORGANIZATION_DOCUMENT)).doesNotThrowAnyException();
    }

    @Test
    void refuseUnFormatValideAilleursMaisPasIci() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0};
        assertThatThrownBy(() -> validator.check(file(jpeg), FileKind.CV))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Utilisez un PDF ou un fichier Word");
    }

    @Test
    void refuseUnFichierAbsentVideOuTropLourd() {
        assertThatThrownBy(() -> validator.check(null, FileKind.CV)).hasMessage("Joignez un fichier.");
        assertThatThrownBy(() -> validator.check(file(new byte[0]), FileKind.CV)).hasMessage("Joignez un fichier.");
        byte[] big = new byte[(int) UploadValidator.MAX_BYTES + 1];
        big[0] = 0x25;
        assertThatThrownBy(() -> validator.check(file(big), FileKind.CV)).hasMessage("Ce fichier dépasse 5 Mo.");
    }

    @Test
    void refuseUnFichierPlusCourtQueLaSignature() {
        assertThatThrownBy(() -> validator.check(file(new byte[] {0x25}), FileKind.CV)).isInstanceOf(ApiException.class);
    }
}
