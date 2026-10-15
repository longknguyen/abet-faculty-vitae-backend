package edu.virginia.cs.abetvitae.extraction.parsing;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.file.Path;

/** Executes parser-worker requests without containing any document parser implementation. */
final class RemoteCvTextExtractor implements CvTextExtractor {

    private final HttpClient httpClient;
    private final ParserWorkerRequestFactory requestFactory;
    private final ParserWorkerResponseDecoder responseDecoder;

    RemoteCvTextExtractor(
            HttpClient httpClient,
            ParserWorkerRequestFactory requestFactory,
            ParserWorkerResponseDecoder responseDecoder) {
        this.httpClient = httpClient;
        this.requestFactory = requestFactory;
        this.responseDecoder = responseDecoder;
    }

    @Override
    public ExtractedCvText extract(Path stagedFile, String originalFilename) {
        try {
            HttpResponse<InputStream> response = httpClient.send(
                    requestFactory.create(stagedFile, originalFilename),
                    HttpResponse.BodyHandlers.ofInputStream());
            return responseDecoder.decode(response.statusCode(), response.body());
        } catch (CvTextExtractionException exception) {
            throw exception;
        } catch (HttpTimeoutException exception) {
            throw failure(
                    CvTextExtractionFailure.WORKER_TIMEOUT,
                    "The isolated document parser exceeded its execution limit",
                    exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw failure(
                    CvTextExtractionFailure.WORKER_UNAVAILABLE,
                    "Document parsing was interrupted",
                    exception);
        } catch (IOException exception) {
            throw failure(
                    CvTextExtractionFailure.WORKER_UNAVAILABLE,
                    "The isolated document parser is unavailable",
                    exception);
        } catch (RuntimeException exception) {
            throw failure(
                    CvTextExtractionFailure.INVALID_WORKER_RESPONSE,
                    "The isolated document parser returned an invalid response",
                    exception);
        }
    }

    private static CvTextExtractionException failure(
            CvTextExtractionFailure failure, String message, Throwable cause) {
        return new CvTextExtractionException(failure, message, cause);
    }
}
