package edu.virginia.cs.abetvitae.extraction.parsing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.exception.EncryptedDocumentException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

/** Coordinates restricted text extraction after a document has passed format validation. */
public final class TikaCvTextExtractor implements CvTextExtractor {

    private final long maximumFileSizeBytes;
    private final int maximumExtractedCharacters;
    private final CvDocumentFormatValidator documentValidator;
    private final TikaParserFactory parserFactory;
    private final TikaExtractionResultFactory resultFactory;

    public TikaCvTextExtractor(
            long maximumFileSizeBytes,
            int maximumExtractedCharacters,
            long maximumPdfMainMemoryBytes) {
        if (maximumFileSizeBytes < 1
                || maximumExtractedCharacters < 1
                || maximumPdfMainMemoryBytes < 1) {
            throw new IllegalArgumentException("CV extraction limits must be positive");
        }
        var detector = new DefaultDetector();
        this.maximumFileSizeBytes = maximumFileSizeBytes;
        this.maximumExtractedCharacters = maximumExtractedCharacters;
        this.documentValidator = new CvDocumentFormatValidator(
                maximumFileSizeBytes, detector);
        this.parserFactory = new TikaParserFactory(detector, maximumPdfMainMemoryBytes);
        this.resultFactory = new TikaExtractionResultFactory();
    }

    @Override
    public ExtractedCvText extract(Path stagedFile, String originalFilename) {
        ValidatedCvDocument document = documentValidator.validate(stagedFile, originalFilename);
        Metadata metadata = newMetadata(document.safeFilename());
        metadata.set(HttpHeaders.CONTENT_TYPE, document.mediaType().toString());
        BodyContentHandler handler = new BodyContentHandler(maximumExtractedCharacters);

        parse(stagedFile, handler, metadata);
        return resultFactory.create(document.mediaType(), handler.toString(), metadata);
    }

    private void parse(Path stagedFile, BodyContentHandler handler, Metadata metadata) {
        try (InputStream raw = Files.newInputStream(stagedFile, LinkOption.NOFOLLOW_LINKS);
                InputStream bounded = new MaximumSizeInputStream(raw, maximumFileSizeBytes)) {
            parserFactory.newParser().parse(
                    bounded, handler, metadata, parserFactory.newParseContext());
        } catch (EncryptedDocumentException exception) {
            throw failure(
                    CvTextExtractionFailure.ENCRYPTED_DOCUMENT,
                    "Encrypted or password-protected documents are not accepted",
                    exception);
        } catch (SAXException exception) {
            handleSaxException(exception);
        } catch (TikaException exception) {
            handleTikaException(exception);
        } catch (MaximumSizeExceededException exception) {
            throw failure(
                    CvTextExtractionFailure.FILE_TOO_LARGE,
                    "The uploaded file exceeds the configured size limit",
                    exception);
        } catch (IOException exception) {
            throw failure(
                    CvTextExtractionFailure.IO_FAILURE,
                    "The uploaded file could not be read",
                    exception);
        }
    }

    private static void handleSaxException(SAXException exception) {
        if (WriteLimitReachedException.isWriteLimitReached(exception)) {
            throw failure(
                    CvTextExtractionFailure.TEXT_LIMIT_EXCEEDED,
                    "The extracted text exceeds the configured character limit",
                    exception);
        }
        throw failure(
                CvTextExtractionFailure.MALFORMED_OR_SUSPICIOUS_DOCUMENT,
                "The document could not be parsed safely",
                exception);
    }

    private static void handleTikaException(TikaException exception) {
        if (hasCause(exception, EncryptedDocumentException.class)) {
            throw failure(
                    CvTextExtractionFailure.ENCRYPTED_DOCUMENT,
                    "Encrypted or password-protected documents are not accepted",
                    exception);
        }
        if (hasCause(exception, MaximumSizeExceededException.class)) {
            throw failure(
                    CvTextExtractionFailure.FILE_TOO_LARGE,
                    "The uploaded file exceeds the configured size limit",
                    exception);
        }
        throw failure(
                CvTextExtractionFailure.MALFORMED_OR_SUSPICIOUS_DOCUMENT,
                "The document could not be parsed safely",
                exception);
    }

    private static Metadata newMetadata(String safeFilename) {
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, safeFilename);
        return metadata;
    }

    private static boolean hasCause(Throwable exception, Class<? extends Throwable> type) {
        Throwable current = exception;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static CvTextExtractionException failure(
            CvTextExtractionFailure failure, String message, Throwable cause) {
        return new CvTextExtractionException(failure, message, cause);
    }
}
