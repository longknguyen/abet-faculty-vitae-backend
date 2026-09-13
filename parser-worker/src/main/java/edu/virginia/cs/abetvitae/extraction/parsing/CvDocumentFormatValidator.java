package edu.virginia.cs.abetvitae.extraction.parsing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;

/** Validates the staged file, filename extension and detected media type before parsing. */
final class CvDocumentFormatValidator {

    private static final MediaType PDF = MediaType.application("pdf");
    private static final MediaType DOCX = MediaType.parse(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    private static final Map<String, MediaType> ALLOWED_EXTENSIONS = Map.of(
            "pdf", PDF,
            "docx", DOCX);

    private final long maximumFileSizeBytes;
    private final Detector detector;

    CvDocumentFormatValidator(long maximumFileSizeBytes, Detector detector) {
        this.maximumFileSizeBytes = maximumFileSizeBytes;
        this.detector = detector;
    }

    ValidatedCvDocument validate(Path stagedFile, String originalFilename) {
        validateSource(stagedFile);
        String safeFilename = sanitiseFilename(originalFilename);
        MediaType expectedMediaType = expectedMediaType(safeFilename);
        MediaType detectedMediaType = detect(stagedFile, safeFilename);

        if (!ALLOWED_EXTENSIONS.containsValue(detectedMediaType)) {
            throw failure(
                    CvTextExtractionFailure.UNSUPPORTED_MEDIA_TYPE,
                    "The uploaded file is not a supported PDF or DOCX document");
        }
        if (!expectedMediaType.equals(detectedMediaType)) {
            throw failure(
                    CvTextExtractionFailure.TYPE_MISMATCH,
                    "The filename extension does not match the detected document type");
        }
        return new ValidatedCvDocument(safeFilename, detectedMediaType);
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
            if (Files.size(stagedFile) > maximumFileSizeBytes) {
                throw failure(
                        CvTextExtractionFailure.FILE_TOO_LARGE,
                        "The uploaded file exceeds the configured size limit");
            }
        } catch (IOException exception) {
            throw new CvTextExtractionException(
                    CvTextExtractionFailure.IO_FAILURE,
                    "The staged upload could not be inspected",
                    exception);
        }
    }

    private MediaType detect(Path stagedFile, String safeFilename) {
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, safeFilename);
        try (InputStream raw = Files.newInputStream(stagedFile, LinkOption.NOFOLLOW_LINKS);
                InputStream bounded = new MaximumSizeInputStream(raw, maximumFileSizeBytes);
                TikaInputStream input = TikaInputStream.get(bounded)) {
            return detector.detect(input, metadata).getBaseType();
        } catch (MaximumSizeExceededException exception) {
            throw new CvTextExtractionException(
                    CvTextExtractionFailure.FILE_TOO_LARGE,
                    "The uploaded file exceeds the configured size limit",
                    exception);
        } catch (IOException exception) {
            throw new CvTextExtractionException(
                    CvTextExtractionFailure.IO_FAILURE,
                    "The uploaded file type could not be detected",
                    exception);
        }
    }

    private static String sanitiseFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw failure(
                    CvTextExtractionFailure.UNSUPPORTED_EXTENSION,
                    "A PDF or DOCX filename is required");
        }
        String normalisedSeparators = originalFilename.replace('\\', '/');
        String basename = normalisedSeparators.substring(normalisedSeparators.lastIndexOf('/') + 1);
        StringBuilder safe = new StringBuilder(Math.min(basename.length(), 255));
        basename.codePoints()
                .filter(codePoint -> !Character.isISOControl(codePoint))
                .limit(255)
                .forEach(safe::appendCodePoint);
        if (safe.isEmpty()) {
            throw failure(
                    CvTextExtractionFailure.UNSUPPORTED_EXTENSION,
                    "A PDF or DOCX filename is required");
        }
        return safe.toString();
    }

    private static MediaType expectedMediaType(String safeFilename) {
        int extensionSeparator = safeFilename.lastIndexOf('.');
        String extension = extensionSeparator < 0
                ? ""
                : safeFilename.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
        MediaType mediaType = ALLOWED_EXTENSIONS.get(extension);
        if (mediaType == null) {
            throw failure(
                    CvTextExtractionFailure.UNSUPPORTED_EXTENSION,
                    "Only .pdf and .docx files are accepted");
        }
        return mediaType;
    }

    private static CvTextExtractionException failure(
            CvTextExtractionFailure failure, String message) {
        return new CvTextExtractionException(failure, message);
    }
}
