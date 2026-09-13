package edu.virginia.cs.abetvitae.extraction.parsing;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;

/** Verifies and sanitises parser output before it leaves the isolated worker. */
final class TikaExtractionResultFactory {

    private static final Set<String> RETAINED_METADATA = Set.of(
            TikaCoreProperties.TITLE.getName(),
            TikaCoreProperties.CREATOR.getName(),
            TikaCoreProperties.CREATED.getName(),
            TikaCoreProperties.MODIFIED.getName());

    ExtractedCvText create(
            MediaType expectedMediaType, String extractedText, Metadata metadata) {
        verifyParsedType(metadata, expectedMediaType);
        String text = normaliseText(extractedText);
        if (text.isBlank()) {
            throw failure(
                    CvTextExtractionFailure.NO_TEXT,
                    "The document did not contain extractable text");
        }
        return new ExtractedCvText(
                expectedMediaType.toString(), text, retainSafeMetadata(metadata));
    }

    private static void verifyParsedType(Metadata metadata, MediaType expectedMediaType) {
        MediaType parsedMediaType = MediaType.parse(metadata.get(HttpHeaders.CONTENT_TYPE));
        if (parsedMediaType == null) {
            throw failure(
                    CvTextExtractionFailure.MALFORMED_OR_SUSPICIOUS_DOCUMENT,
                    "The parser did not report a document type");
        }
        if (!expectedMediaType.equals(parsedMediaType.getBaseType())) {
            throw failure(
                    CvTextExtractionFailure.TYPE_MISMATCH,
                    "The document type changed during parsing");
        }
    }

    private static Map<String, List<String>> retainSafeMetadata(Metadata metadata) {
        Map<String, List<String>> retained = new LinkedHashMap<>();
        for (String name : RETAINED_METADATA) {
            String[] values = metadata.getValues(name);
            if (values.length > 0) {
                retained.put(name, List.copyOf(Arrays.asList(values)));
            }
        }
        return retained;
    }

    private static String normaliseText(String text) {
        String withoutNulls = text.replace("\u0000", "");
        String withUnixLineEndings = withoutNulls.replace("\r\n", "\n").replace('\r', '\n');
        return Normalizer.normalize(withUnixLineEndings, Normalizer.Form.NFC).strip();
    }

    private static CvTextExtractionException failure(
            CvTextExtractionFailure failure, String message) {
        return new CvTextExtractionException(failure, message);
    }
}
