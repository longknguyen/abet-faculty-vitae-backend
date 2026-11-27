package edu.virginia.cs.abetvitae.extraction.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemPrivateCvStorageTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void storesAndRetrievesOwnerOnlyFile() throws Exception {
        Path storageDirectory = temporaryDirectory.resolve("private");
        FileSystemPrivateCvStorage storage =
                new FileSystemPrivateCvStorage(storageDirectory.toString());
        byte[] content = "%PDF-fixture".getBytes(StandardCharsets.US_ASCII);

        StoredCvFile stored = storage.store(
                new ByteArrayInputStream(content),
                1024,
                "application/pdf"
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        long copiedBytes = storage.copyTo(
                stored.storageKey(),
                output,
                1024
        );

        assertThat(copiedBytes).isEqualTo(content.length);
        assertThat(output.toByteArray()).isEqualTo(content);
        assertThat(Files.getPosixFilePermissions(
                storageDirectory.resolve(stored.storageKey())
        )).isEqualTo(PosixFilePermissions.fromString("rw-------"));
    }

    @Test
    void deletesPartialFileWhenStoreExceedsLimit() throws Exception {
        Path storageDirectory = temporaryDirectory.resolve("private");
        FileSystemPrivateCvStorage storage =
                new FileSystemPrivateCvStorage(storageDirectory.toString());

        assertThatThrownBy(() -> storage.store(
                new ByteArrayInputStream(new byte[5]),
                4,
                "application/pdf"
        )).isInstanceOf(IOException.class);

        try (var files = Files.list(storageDirectory)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void rejectsInvalidStorageKey() throws Exception {
        FileSystemPrivateCvStorage storage = new FileSystemPrivateCvStorage(
                temporaryDirectory.resolve("private").toString()
        );

        assertThatThrownBy(() -> storage.copyTo(
                "../outside",
                new ByteArrayOutputStream(),
                1024
        )).isInstanceOf(IOException.class)
                .hasMessage("Invalid storage key");
    }
}
