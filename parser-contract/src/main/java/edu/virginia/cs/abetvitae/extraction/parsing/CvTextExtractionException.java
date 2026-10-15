package edu.virginia.cs.abetvitae.extraction.parsing;

import java.io.Serial;
import java.util.Objects;

/** An expected document-extraction failure. Messages deliberately omit file content and paths. */
public final class CvTextExtractionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final CvTextExtractionFailure failure;

    public CvTextExtractionException(CvTextExtractionFailure failure, String message) {
        super(message);
        this.failure = Objects.requireNonNull(failure);
    }

    public CvTextExtractionException(
            CvTextExtractionFailure failure, String message, Throwable cause) {
        super(message, cause);
        this.failure = Objects.requireNonNull(failure);
    }

    public CvTextExtractionFailure getFailure() {
        return failure;
    }
}
