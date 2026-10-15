package edu.virginia.cs.abetvitae.parserworker;

import edu.virginia.cs.abetvitae.extraction.parsing.CvTextExtractor;
import edu.virginia.cs.abetvitae.extraction.parsing.ExtractedCvText;
import jakarta.annotation.PreDestroy;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

/** Allows one parse at a time and kills the disposable worker if Tika exceeds its deadline. */
@Service
final class BoundedTikaExtractionService {

    private final CvTextExtractor extractor;
    private final ParserWorkerProperties properties;
    private final ParserWorkerProcessTerminator processTerminator;
    private final Semaphore parserPermit = new Semaphore(1);
    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(
            runnable -> Thread.ofPlatform()
                    .daemon(true)
                    .name("parser-worker-watchdog")
                    .unstarted(runnable));

    BoundedTikaExtractionService(
            CvTextExtractor extractor,
            ParserWorkerProperties properties,
            ParserWorkerProcessTerminator processTerminator) {
        this.extractor = extractor;
        this.properties = properties;
        this.processTerminator = processTerminator;
    }

    ExtractedCvText extract(Path stagedFile, String originalFilename) {
        if (!parserPermit.tryAcquire()) {
            throw new ParserWorkerBusyException();
        }

        ScheduledFuture<?> deadline = watchdog.schedule(
                processTerminator::terminate,
                properties.maximumExecutionTime().toMillis(),
                TimeUnit.MILLISECONDS);
        try {
            return extractor.extract(stagedFile, originalFilename);
        } finally {
            deadline.cancel(false);
            parserPermit.release();
        }
    }

    @PreDestroy
    void stopWatchdog() {
        watchdog.shutdownNow();
    }
}
