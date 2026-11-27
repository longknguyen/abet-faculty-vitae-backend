package edu.virginia.cs.abetvitae.extraction.messaging;

import java.io.Serial;

final class CvMessagePublicationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    CvMessagePublicationException(String message, Throwable cause) {
        super(message, cause);
    }

    CvMessagePublicationException(String message) {
        super(message);
    }
}
