package edu.virginia.cs.abetvitae.extraction.service;

import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Safe client-facing rejection that never includes document content or storage details. */
public final class CvUploadRejectedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final HttpStatus status;

    private CvUploadRejectedException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public static CvUploadRejectedException badRequest(String message) {
        return new CvUploadRejectedException(HttpStatus.BAD_REQUEST, message, null);
    }

    public static CvUploadRejectedException payloadTooLarge(String message) {
        return new CvUploadRejectedException(HttpStatus.CONTENT_TOO_LARGE, message, null);
    }

    public static CvUploadRejectedException storageUnavailable(Throwable cause) {
        return new CvUploadRejectedException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "The uploaded CV could not be stored. Please try again.",
                cause);
    }

    public HttpStatus status() {
        return status;
    }
}
