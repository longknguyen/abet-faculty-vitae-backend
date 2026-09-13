package edu.virginia.cs.abetvitae.parserworker;

import edu.virginia.cs.abetvitae.extraction.parsing.CvTextExtractionFailure;

final class InvalidWorkerRequestException extends RuntimeException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final CvTextExtractionFailure failure;

    InvalidWorkerRequestException(CvTextExtractionFailure failure, String message) {
        super(message);
        this.failure = failure;
    }

    CvTextExtractionFailure failure() {
        return failure;
    }
}
