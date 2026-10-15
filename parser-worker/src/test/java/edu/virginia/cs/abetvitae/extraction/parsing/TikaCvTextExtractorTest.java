package edu.virginia.cs.abetvitae.extraction.parsing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.tika.metadata.TikaCoreProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TikaCvTextExtractorTest {

    private static final long TEST_FILE_LIMIT = 2_000_000L;
    private static final int TEST_TEXT_LIMIT = 10_000;
    private static final long TEST_PDF_MEMORY_LIMIT = 1_000_000L;

    @TempDir
    private Path temporaryDirectory;

    @Test
    void extractsTextAndAllowListedMetadataFromPdf() throws IOException {
        Path file = temporaryDirectory.resolve("cv.pdf");
        createPdf(file, "ABET faculty curriculum vitae", false);

        ExtractedCvText result = extractor().extract(file, "cv.pdf");

        assertThat(result.detectedMediaType()).isEqualTo("application/pdf");
        assertThat(result.text()).contains("ABET faculty curriculum vitae");
        assertThat(result.metadata().keySet())
                .containsOnly(TikaCoreProperties.TITLE.getName());
    }

    @Test
    void extractsTextFromDocx() throws IOException {
        Path file = temporaryDirectory.resolve("cv.docx");
        createDocx(file, "Secure DOCX extraction");

        ExtractedCvText result = extractor().extract(file, "cv.docx");

        assertThat(result.detectedMediaType()).isEqualTo(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(result.text()).contains("Secure DOCX extraction");
    }

    @Test
    void rejectsContentThatDoesNotMatchAnAllowedMediaType() throws IOException {
        Path file = temporaryDirectory.resolve("malware.pdf");
        Files.writeString(file, "MZ not really a PDF", StandardCharsets.UTF_8);

        assertFailure(file, "malware.pdf", CvTextExtractionFailure.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void rejectsAValidDocumentWithTheWrongExtension() throws IOException {
        Path file = temporaryDirectory.resolve("cv.docx");
        createPdf(file, "PDF content", false);

        assertFailure(file, "cv.docx", CvTextExtractionFailure.TYPE_MISMATCH);
    }

    @Test
    void rejectsEncryptedPdf() throws IOException {
        Path file = temporaryDirectory.resolve("encrypted.pdf");
        createPdf(file, "Confidential CV", true);

        assertFailure(file, "encrypted.pdf", CvTextExtractionFailure.ENCRYPTED_DOCUMENT);
    }

    @Test
    void rejectsDocumentsWhoseExtractedTextExceedsTheLimit() throws IOException {
        Path file = temporaryDirectory.resolve("large-text.pdf");
        createPdf(file, "A".repeat(500), false);
        TikaCvTextExtractor extractor =
                new TikaCvTextExtractor(TEST_FILE_LIMIT, 50, TEST_PDF_MEMORY_LIMIT);

        assertThatThrownBy(() -> extractor.extract(file, "large-text.pdf"))
                .isInstanceOfSatisfying(CvTextExtractionException.class, exception ->
                        assertThat(exception.getFailure())
                                .isEqualTo(CvTextExtractionFailure.TEXT_LIMIT_EXCEEDED));
    }

    @Test
    void rejectsFilesThatExceedTheByteLimit() throws IOException {
        Path file = temporaryDirectory.resolve("large.pdf");
        createPdf(file, "A faculty CV", false);
        long smallerThanFile = Files.size(file) - 1;
        TikaCvTextExtractor extractor =
                new TikaCvTextExtractor(smallerThanFile, TEST_TEXT_LIMIT, TEST_PDF_MEMORY_LIMIT);

        assertThatThrownBy(() -> extractor.extract(file, "large.pdf"))
                .isInstanceOfSatisfying(CvTextExtractionException.class, exception ->
                        assertThat(exception.getFailure())
                                .isEqualTo(CvTextExtractionFailure.FILE_TOO_LARGE));
    }

    @Test
    void rejectsPdfWithoutExtractableText() throws IOException {
        Path file = temporaryDirectory.resolve("empty.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(file.toFile());
        }

        assertFailure(file, "empty.pdf", CvTextExtractionFailure.NO_TEXT);
    }

    @Test
    void rejectsUnsupportedFilenameExtensionBeforeParsing() throws IOException {
        Path file = temporaryDirectory.resolve("legacy.doc");
        Files.writeString(file, "not parsed", StandardCharsets.UTF_8);

        assertFailure(file, "legacy.doc", CvTextExtractionFailure.UNSUPPORTED_EXTENSION);
    }

    private TikaCvTextExtractor extractor() {
        return new TikaCvTextExtractor(
                TEST_FILE_LIMIT, TEST_TEXT_LIMIT, TEST_PDF_MEMORY_LIMIT);
    }

    private void assertFailure(
            Path file, String filename, CvTextExtractionFailure expectedFailure) {
        assertThatThrownBy(() -> extractor().extract(file, filename))
                .isInstanceOfSatisfying(CvTextExtractionException.class, exception ->
                        assertThat(exception.getFailure()).isEqualTo(expectedFailure));
    }

    private static void createPdf(Path path, String text, boolean encrypted) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            document.getDocumentInformation().setTitle("Faculty CV");
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText(text);
                content.endText();
            }
            if (encrypted) {
                StandardProtectionPolicy policy = new StandardProtectionPolicy(
                        "owner-password", "user-password", new AccessPermission());
                policy.setEncryptionKeyLength(128);
                document.protect(policy);
            }
            document.save(path.toFile());
        }
    }

    private static void createDocx(Path path, String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
                OutputStream output = Files.newOutputStream(path)) {
            document.createParagraph().createRun().setText(text);
            document.write(output);
        }
    }
}
