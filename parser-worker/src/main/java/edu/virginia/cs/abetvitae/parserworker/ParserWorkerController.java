package edu.virginia.cs.abetvitae.parserworker;

import edu.virginia.cs.abetvitae.extraction.parsing.CvTextExtractionFailure;
import edu.virginia.cs.abetvitae.extraction.parsing.ExtractedCvText;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Adapts the private worker HTTP request to the bounded extraction use case. */
@RestController
@RequestMapping("/internal/v1/extract")
final class ParserWorkerController {

    static final String ORIGINAL_FILENAME_HEADER = "X-Original-Filename-Base64";

    private final BoundedTikaExtractionService extractionService;
    private final ParserWorkerRequestValidator requestValidator;
    private final TemporaryCvFileStore temporaryFileStore;

    ParserWorkerController(
            BoundedTikaExtractionService extractionService,
            ParserWorkerRequestValidator requestValidator,
            TemporaryCvFileStore temporaryFileStore) {
        this.extractionService = extractionService;
        this.requestValidator = requestValidator;
        this.temporaryFileStore = temporaryFileStore;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ExtractedCvText extract(
            @RequestHeader(ORIGINAL_FILENAME_HEADER) String encodedFilename,
            HttpServletRequest request) {
        requestValidator.validateContentLength(request.getContentLengthLong());
        String originalFilename = requestValidator.decodeFilename(encodedFilename);
        Path stagedFile = temporaryFileStore.stage(requestBody(request));
        try {
            return extractionService.extract(stagedFile, originalFilename);
        } finally {
            temporaryFileStore.delete(stagedFile);
        }
    }

    private static InputStream requestBody(HttpServletRequest request) {
        try {
            return request.getInputStream();
        } catch (IOException exception) {
            throw new InvalidWorkerRequestException(
                    CvTextExtractionFailure.IO_FAILURE,
                    "The upload body could not be read");
        }
    }
}
