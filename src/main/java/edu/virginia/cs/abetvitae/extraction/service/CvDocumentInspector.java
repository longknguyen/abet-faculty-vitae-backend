package edu.virginia.cs.abetvitae.extraction.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Performs cheap bounded screening without loading a document parser into the web process. */
@Component
final class CvDocumentInspector {

    private static final int MAXIMUM_FILENAME_CODE_POINTS = 255;
    private static final int SIGNATURE_PROBE_BYTES = 1_024;
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final Map<String, String> MEDIA_TYPES = Map.of(
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final CvUploadProperties properties;

    CvDocumentInspector(CvUploadProperties properties) {
        this.properties = properties;
    }

    InspectedCvDocument inspect(MultipartFile file) {
        validateSize(file);
        String filename = sanitiseFilename(file.getOriginalFilename());
        String extension = extension(filename);
        String mediaType = MEDIA_TYPES.get(extension);
        if (mediaType == null) {
            throw CvUploadRejectedException.badRequest(
                    "Only PDF and DOCX files are accepted.");
        }

        byte[] probe = readProbe(file);
        boolean plausible = "pdf".equals(extension)
                ? contains(probe, PDF_SIGNATURE)
                : hasZipSignature(probe);
        if (!plausible) {
            throw CvUploadRejectedException.badRequest(
                    "The filename extension does not match the uploaded document.");
        }
        return new InspectedCvDocument(filename, mediaType);
    }

    private void validateSize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw CvUploadRejectedException.badRequest("Please select a CV to upload.");
        }
        if (file.getSize() > properties.maximumFileSizeBytes()) {
            throw CvUploadRejectedException.payloadTooLarge(
                    "The uploaded CV exceeds the configured size limit.");
        }
    }

    private static String sanitiseFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw CvUploadRejectedException.badRequest(
                    "The uploaded CV must have a filename.");
        }
        String normalisedSeparators = originalFilename.replace('\\', '/');
        String basename = normalisedSeparators.substring(
                normalisedSeparators.lastIndexOf('/') + 1).strip();
        StringBuilder safe = new StringBuilder(Math.min(basename.length(), 255));
        basename.codePoints()
                .filter(codePoint -> !Character.isISOControl(codePoint))
                .forEach(safe::appendCodePoint);
        if (safe.isEmpty()) {
            throw CvUploadRejectedException.badRequest(
                    "The uploaded CV must have a filename.");
        }
        if (safe.codePointCount(0, safe.length())
                > MAXIMUM_FILENAME_CODE_POINTS) {
            throw CvUploadRejectedException.badRequest(
                    "The uploaded CV filename is too long.");
        }
        return safe.toString();
    }

    private static String extension(String filename) {
        int separator = filename.lastIndexOf('.');
        return separator < 0
                ? ""
                : filename.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    private static byte[] readProbe(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            return input.readNBytes(SIGNATURE_PROBE_BYTES);
        } catch (IOException exception) {
            throw CvUploadRejectedException.badRequest(
                    "The uploaded CV could not be inspected.");
        }
    }

    private static boolean contains(byte[] value, byte[] expected) {
        int maximumStart = value.length - expected.length;
        for (int start = 0; start <= maximumStart; start++) {
            boolean matches = true;
            for (int offset = 0; offset < expected.length; offset++) {
                if (value[start + offset] != expected[offset]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasZipSignature(byte[] value) {
        return value.length >= 4
                && value[0] == 'P'
                && value[1] == 'K'
                && (value[2] == 3 || value[2] == 5 || value[2] == 7)
                && (value[3] == 4 || value[3] == 6 || value[3] == 8);
    }
}
