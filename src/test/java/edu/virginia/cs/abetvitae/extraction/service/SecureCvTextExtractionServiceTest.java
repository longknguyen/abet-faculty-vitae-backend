package edu.virginia.cs.abetvitae.extraction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.virginia.cs.abetvitae.extraction.parsing.CvTextExtractor;
import edu.virginia.cs.abetvitae.extraction.parsing.ExtractedCvText;
import edu.virginia.cs.abetvitae.extraction.scanning.MalwareDetectedException;
import edu.virginia.cs.abetvitae.extraction.scanning.MalwareScanResult;
import edu.virginia.cs.abetvitae.extraction.scanning.MalwareScanner;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class SecureCvTextExtractionServiceTest {

    private static final Path STAGED_FILE = Path.of("staged-upload");

    @Test
    void extractsOnlyAfterAConclusiveCleanScan() {
        ExtractedCvText expected = new ExtractedCvText("application/pdf", "CV text", Map.of());
        MalwareScanner scanner = file -> MalwareScanResult.clean();
        CvTextExtractor extractor = (file, filename) -> expected;
        SecureCvTextExtractionService service =
                new SecureCvTextExtractionService(scanner, extractor);

        ExtractedCvText actual = service.scanAndExtract(STAGED_FILE, "cv.pdf");

        assertThat(actual).isSameAs(expected);
    }

    @Test
    void neverParsesAnInfectedDocument() {
        AtomicBoolean parserCalled = new AtomicBoolean();
        MalwareScanner scanner = file -> MalwareScanResult.infected("Test.Signature");
        CvTextExtractor extractor = (file, filename) -> {
            parserCalled.set(true);
            return new ExtractedCvText("application/pdf", "unsafe", Map.of());
        };
        SecureCvTextExtractionService service =
                new SecureCvTextExtractionService(scanner, extractor);

        assertThatThrownBy(() -> service.scanAndExtract(STAGED_FILE, "cv.pdf"))
                .isInstanceOfSatisfying(MalwareDetectedException.class, exception ->
                        assertThat(exception.getSignature()).isEqualTo("Test.Signature"));
        assertThat(parserCalled).isFalse();
    }
}
