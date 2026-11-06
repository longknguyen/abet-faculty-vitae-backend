package edu.virginia.cs.abetvitae.extraction.messaging;

import java.io.Serial;

final class CvProcessingRetryableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    CvProcessingRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
