package edu.virginia.cs.abetvitae.extraction.service;

import edu.virginia.cs.abetvitae.extraction.parsing.CvTextExtractor;
import edu.virginia.cs.abetvitae.extraction.parsing.ExtractedCvText;
import edu.virginia.cs.abetvitae.extraction.scanning.MalwareDetectedException;
import edu.virginia.cs.abetvitae.extraction.scanning.MalwareScanResult;
import edu.virginia.cs.abetvitae.extraction.scanning.MalwareScanner;
import java.nio.file.Path;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Enforces the invariant that untrusted CVs are conclusively clean before Tika sees them. */
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@ConditionalOnProperty(name = "app.cv-extraction.worker.enabled", havingValue = "true")
public final class SecureCvTextExtractionService {

    private final MalwareScanner malwareScanner;
    private final CvTextExtractor textExtractor;

    public ExtractedCvText scanAndExtract(Path stagedFile, String originalFilename) {
        MalwareScanResult scanResult = malwareScanner.scan(stagedFile);

        if (!scanResult.isClean()) {
            throw new MalwareDetectedException(scanResult.signature());
        }

        return textExtractor.extract(stagedFile, originalFilename);
    }
}
