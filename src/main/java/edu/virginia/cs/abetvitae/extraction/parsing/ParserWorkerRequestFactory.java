package edu.virginia.cs.abetvitae.extraction.parsing;

import java.io.FileNotFoundException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;

/** Validates a staged CV and creates its bounded parser-worker request. */
final class ParserWorkerRequestFactory {

    static final String ORIGINAL_FILENAME_HEADER = "X-Original-Filename-Base64";
    private static final String EXTRACTION_MEDIA_TYPE = "application/octet-stream";
    private static final int MAXIMUM_FILENAME_CODE_POINTS = 255;

    private final CvParserWorkerProperties properties;

    ParserWorkerRequestFactory(CvParserWorkerProperties properties) {
        this.properties = properties;
    }

    HttpRequest create(Path stagedFile, String originalFilename) throws FileNotFoundException {
        validateSource(stagedFile);
        String encodedFilename = encodeFilename(originalFilename);
        return HttpRequest.newBuilder(properties.extractionEndpoint())
                .timeout(properties.requestTimeout())
                .header("Authorization", "Bearer " + properties.token())
                .header("Content-Type", EXTRACTION_MEDIA_TYPE)
                .header(ORIGINAL_FILENAME_HEADER, encodedFilename)
                .POST(HttpRequest.BodyPublishers.ofFile(stagedFile))
                .build();
    }

    private void validateSource(Path stagedFile) {
        if (stagedFile == null
                || Files.isSymbolicLink(stagedFile)
                || !Files.isRegularFile(stagedFile, LinkOption.NOFOLLOW_LINKS)) {
            throw failure(
                    CvTextExtractionFailure.INVALID_SOURCE,
                    "The staged upload is not a regular file");
        }
        try {
            if (Files.size(stagedFile) > properties.maximumFileSizeBytes()) {
                throw failure(
                        CvTextExtractionFailure.FILE_TOO_LARGE,
                        "The uploaded file exceeds the configured size limit");
            }
        } catch (java.io.IOException exception) {
            throw new CvTextExtractionException(
                    CvTextExtractionFailure.IO_FAILURE,
                    "The staged upload could not be inspected",
                    exception);
        }
    }

    private static String encodeFilename(String originalFilename) {
        String safeFilename = sanitiseFilename(originalFilename);
        String lowerCaseFilename = safeFilename.toLowerCase(Locale.ROOT);
        if (!(lowerCaseFilename.endsWith(".pdf") || lowerCaseFilename.endsWith(".docx"))) {
            throw failure(
                    CvTextExtractionFailure.UNSUPPORTED_EXTENSION,
                    "Only .pdf and .docx files are accepted");
        }
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(safeFilename.getBytes(StandardCharsets.UTF_8));
    }

    private static String sanitiseFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw failure(
                    CvTextExtractionFailure.UNSUPPORTED_EXTENSION,
                    "A PDF or DOCX filename is required");
        }
        String normalisedSeparators = originalFilename.replace('\\', '/');
        String basename = normalisedSeparators.substring(normalisedSeparators.lastIndexOf('/') + 1);
        StringBuilder safe = new StringBuilder(
                Math.min(basename.length(), MAXIMUM_FILENAME_CODE_POINTS));
        basename.codePoints()
                .filter(codePoint -> !Character.isISOControl(codePoint))
                .limit(MAXIMUM_FILENAME_CODE_POINTS)
                .forEach(safe::appendCodePoint);
        if (safe.isEmpty()) {
            throw failure(
                    CvTextExtractionFailure.UNSUPPORTED_EXTENSION,
                    "A PDF or DOCX filename is required");
        }
        return safe.toString();
    }

    private static CvTextExtractionException failure(
            CvTextExtractionFailure failure, String message) {
        return new CvTextExtractionException(failure, message);
    }
}
