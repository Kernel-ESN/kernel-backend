package com.kernelconsulting.plateforme.common.storage;

import java.util.List;
import lombok.Getter;

/** Formats acceptés, reconnus à leur contenu et non à leur nom. */
@Getter
public enum FileKind {
    CV(List.of(Signature.PDF, Signature.DOC, Signature.DOCX), "Utilisez un PDF ou un fichier Word."),
    ORGANIZATION_DOCUMENT(List.of(Signature.PDF, Signature.JPEG, Signature.PNG), "Utilisez un PDF, un JPEG ou un PNG.");

    private final List<Signature> signatures;
    private final String advice;

    FileKind(List<Signature> signatures, String advice) {
        this.signatures = signatures;
        this.advice = advice;
    }

    /** Premiers octets caractéristiques d'un format. */
    public enum Signature {
        PDF(new byte[] {0x25, 0x50, 0x44, 0x46}),
        DOC(new byte[] {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}),
        DOCX(new byte[] {0x50, 0x4B, 0x03, 0x04}),
        JPEG(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
        PNG(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47});

        private final byte[] bytes;

        Signature(byte[] bytes) {
            this.bytes = bytes;
        }

        boolean matches(byte[] head, int length) {
            if (length < bytes.length) {
                return false;
            }
            for (int i = 0; i < bytes.length; i++) {
                if (head[i] != bytes[i]) {
                    return false;
                }
            }
            return true;
        }
    }
}
