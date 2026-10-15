package edu.virginia.cs.abetvitae.extraction.parsing;

/** Stable failure categories shared by the backend and the isolated parser worker. */
public enum CvTextExtractionFailure {
    INVALID_SOURCE,
    FILE_TOO_LARGE,
    UNSUPPORTED_EXTENSION,
    UNSUPPORTED_MEDIA_TYPE,
    TYPE_MISMATCH,
    ENCRYPTED_DOCUMENT,
    TEXT_LIMIT_EXCEEDED,
    NO_TEXT,
    MALFORMED_OR_SUSPICIOUS_DOCUMENT,
    IO_FAILURE,
    WORKER_UNAVAILABLE,
    WORKER_TIMEOUT,
    INVALID_WORKER_RESPONSE
}
