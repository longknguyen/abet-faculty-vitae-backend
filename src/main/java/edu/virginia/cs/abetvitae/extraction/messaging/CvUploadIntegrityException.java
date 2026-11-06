package edu.virginia.cs.abetvitae.extraction.messaging;

import java.io.Serial;

final class CvUploadIntegrityException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    CvUploadIntegrityException() {
        super("The stored CV failed its integrity check");
    }
}
