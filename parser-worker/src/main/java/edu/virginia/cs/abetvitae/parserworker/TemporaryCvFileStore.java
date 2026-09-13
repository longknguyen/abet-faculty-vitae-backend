package edu.virginia.cs.abetvitae.parserworker;

import edu.virginia.cs.abetvitae.extraction.parsing.CvTextExtractionFailure;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Owns the complete lifecycle of a CV staged in the worker's bounded tmpfs. */
@Component
final class TemporaryCvFileStore {

    private static final Set<PosixFilePermission> OWNER_ONLY = Set.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
    private static final int COPY_BUFFER_SIZE = 8_192;

    private final ParserWorkerProperties properties;
    private final ParserWorkerProcessTerminator processTerminator;

    TemporaryCvFileStore(
            ParserWorkerProperties properties,
            ParserWorkerProcessTerminator processTerminator) {
        this.properties = properties;
        this.processTerminator = processTerminator;
    }

    Path stage(InputStream requestBody) {
        Path stagedFile = null;
        try (requestBody) {
            Files.createDirectories(properties.temporaryDirectory());
            stagedFile = Files.createTempFile(
                    properties.temporaryDirectory(), "cv-", ".upload");
            setOwnerOnlyPermissions(stagedFile);
            copyBounded(requestBody, stagedFile);
            return stagedFile;
        } catch (InvalidWorkerRequestException exception) {
            deleteAfterFailedStaging(stagedFile);
            throw exception;
        } catch (IOException exception) {
            deleteAfterFailedStaging(stagedFile);
            throw new InvalidWorkerRequestException(
                    CvTextExtractionFailure.IO_FAILURE,
                    "The upload could not be staged safely");
        }
    }

    void delete(Path stagedFile) {
        try {
            Files.deleteIfExists(stagedFile);
        } catch (IOException exception) {
            // Halting causes the disposable container and its tmpfs to be recreated cleanly.
            processTerminator.terminate();
        }
    }

    private void copyBounded(InputStream input, Path destination) throws IOException {
        try (var output = Files.newOutputStream(destination)) {
            byte[] buffer = new byte[COPY_BUFFER_SIZE];
            long copied = 0L;
            int read;
            while ((read = input.read(buffer)) != -1) {
                copied += read;
                if (copied > properties.maximumFileSizeBytes()) {
                    throw new InvalidWorkerRequestException(
                            CvTextExtractionFailure.FILE_TOO_LARGE,
                            "The uploaded file exceeds the configured size limit");
                }
                output.write(buffer, 0, read);
            }
        }
    }

    private void deleteAfterFailedStaging(Path stagedFile) {
        if (stagedFile != null) {
            delete(stagedFile);
        }
    }

    private static void setOwnerOnlyPermissions(Path stagedFile) throws IOException {
        try {
            Files.setPosixFilePermissions(stagedFile, OWNER_ONLY);
        } catch (UnsupportedOperationException ignored) {
            // The production Linux filesystem supports POSIX permissions.
        }
    }
}
