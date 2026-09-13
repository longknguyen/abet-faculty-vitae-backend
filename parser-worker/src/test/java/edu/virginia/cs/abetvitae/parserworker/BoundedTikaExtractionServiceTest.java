package edu.virginia.cs.abetvitae.parserworker;

import static org.assertj.core.api.Assertions.assertThat;

import edu.virginia.cs.abetvitae.extraction.parsing.CvTextExtractor;
import edu.virginia.cs.abetvitae.extraction.parsing.ExtractedCvText;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BoundedTikaExtractionServiceTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void invokesTheProcessTerminatorWhenParsingExceedsItsDeadline()
            throws InterruptedException {
        CountDownLatch terminated = new CountDownLatch(1);
        ParserWorkerProcessTerminator terminator = new ParserWorkerProcessTerminator() {
            @Override
            void terminate() {
                terminated.countDown();
            }
        };
        CvTextExtractor slowExtractor = (file, filename) -> {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return new ExtractedCvText("application/pdf", "text", Map.of());
        };
        ParserWorkerProperties properties = new ParserWorkerProperties(
                "0123456789abcdef0123456789abcdef",
                1_000_000L,
                10_000,
                1_000_000L,
                Duration.ofMillis(10),
                temporaryDirectory);
        BoundedTikaExtractionService service = new BoundedTikaExtractionService(
                slowExtractor, properties, terminator);

        service.extract(temporaryDirectory.resolve("cv.pdf"), "cv.pdf");

        assertThat(terminated.await(1, TimeUnit.SECONDS)).isTrue();
        service.stopWatchdog();
    }
}
