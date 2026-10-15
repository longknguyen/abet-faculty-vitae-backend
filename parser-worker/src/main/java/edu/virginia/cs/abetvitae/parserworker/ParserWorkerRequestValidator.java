package edu.virginia.cs.abetvitae.parserworker;

import edu.virginia.cs.abetvitae.extraction.parsing.CvTextExtractionFailure;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Component;

/** Validates worker request metadata before any uploaded bytes are staged. */
@Component
final class ParserWorkerRequestValidator {

    private static final int MAXIMUM_ENCODED_FILENAME_LENGTH = 1_024;
    private final long maximumFileSizeBytes;

    ParserWorkerRequestValidator(ParserWorkerProperties properties) {
        maximumFileSizeBytes = properties.maximumFileSizeBytes();
    }

    void validateContentLength(long contentLength) {
        if (contentLength < 1) {
            throw invalid(CvTextExtractionFailure.INVALID_SOURCE, "The upload is empty");
        }
        if (contentLength > maximumFileSizeBytes) {
            throw invalid(
                    CvTextExtractionFailure.FILE_TOO_LARGE,
                    "The uploaded file exceeds the configured size limit");
        }
    }

    String decodeFilename(String encodedFilename) {
        if (encodedFilename.isBlank()
                || encodedFilename.length() > MAXIMUM_ENCODED_FILENAME_LENGTH) {
            throw invalid(
                    CvTextExtractionFailure.UNSUPPORTED_EXTENSION,
                    "A valid PDF or DOCX filename is required");
        }
        try {
            return new String(
                    Base64.getUrlDecoder().decode(encodedFilename), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw invalid(
                    CvTextExtractionFailure.UNSUPPORTED_EXTENSION,
                    "A valid PDF or DOCX filename is required");
        }
    }

    private static InvalidWorkerRequestException invalid(
            CvTextExtractionFailure failure, String message) {
        return new InvalidWorkerRequestException(failure, message);
    }
}
