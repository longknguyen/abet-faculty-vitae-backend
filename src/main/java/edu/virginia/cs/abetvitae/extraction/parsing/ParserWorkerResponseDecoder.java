package edu.virginia.cs.abetvitae.extraction.parsing;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;

/** Bounds and translates the parser worker's private HTTP response. */
final class ParserWorkerResponseDecoder {

    private final int maximumResponseBytes;
    private final ObjectMapper objectMapper;

    ParserWorkerResponseDecoder(
            CvParserWorkerProperties properties, ObjectMapper objectMapper) {
        this.maximumResponseBytes = properties.maximumResponseBytes();
        this.objectMapper = objectMapper;
    }

    ExtractedCvText decode(int statusCode, InputStream responseBody) throws IOException {
        byte[] body = readBounded(responseBody);
        if (statusCode == 200) {
            WorkerSuccess success = objectMapper.readValue(body, WorkerSuccess.class);
            return new ExtractedCvText(
                    success.detectedMediaType(), success.text(), success.metadata());
        }
        throw workerFailure(statusCode, body);
    }

    private byte[] readBounded(InputStream responseBody) throws IOException {
        try (responseBody) {
            byte[] bytes = responseBody.readNBytes(maximumResponseBytes + 1);
            if (bytes.length > maximumResponseBytes) {
                throw failure(
                        CvTextExtractionFailure.INVALID_WORKER_RESPONSE,
                        "The isolated document parser returned too much data");
            }
            return bytes;
        }
    }

    private CvTextExtractionException workerFailure(int statusCode, byte[] body) {
        if (statusCode == 401 || statusCode == 403 || statusCode >= 500) {
            return failure(
                    CvTextExtractionFailure.WORKER_UNAVAILABLE,
                    "The isolated document parser rejected or could not process the request");
        }
        try {
            WorkerError error = objectMapper.readValue(body, WorkerError.class);
            return failure(CvTextExtractionFailure.valueOf(error.failure()), error.message());
        } catch (RuntimeException exception) {
            return new CvTextExtractionException(
                    CvTextExtractionFailure.INVALID_WORKER_RESPONSE,
                    "The isolated document parser returned an invalid error response",
                    exception);
        }
    }

    private static CvTextExtractionException failure(
            CvTextExtractionFailure failure, String message) {
        return new CvTextExtractionException(failure, message);
    }

    private record WorkerSuccess(
            String detectedMediaType, String text, Map<String, List<String>> metadata) {}

    private record WorkerError(String failure, String message) {}
}
