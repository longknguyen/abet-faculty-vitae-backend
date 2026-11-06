package edu.virginia.cs.abetvitae.extraction.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public final class FileSystemPrivateCvStorage implements PrivateCvStorage {
    private final Path storageDirectory;

    public FileSystemPrivateCvStorage(String storageDirectory)
            throws IOException {
        this.storageDirectory = Path.of(storageDirectory)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(
                    this.storageDirectory,
                    PosixFilePermissions.asFileAttribute(
                            PosixFilePermissions.fromString("rwx------")
                    )
            );

            Files.setPosixFilePermissions(
                    this.storageDirectory,
                    PosixFilePermissions.fromString("rwx------")
            );

        } catch (UnsupportedOperationException exception) {
            throw new IOException(
                    "Private CV storage requires POSIX file permissions",
                    exception
            );
        }
    }

    @Override
    public StoredCvFile store(
            InputStream inputStream,
            long maximumBytes,
            String contentType
    ) throws IOException {
        String storageKey = UUID.randomUUID().toString();
        Path target = storageDirectory.resolve(storageKey).normalize();

        if (!storageDirectory.equals(target.getParent())) {
            throw new IOException("Invalid storage path");
        }

        MessageDigest digest;

        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }

        long totalBytes = 0;
        byte[] buffer = new byte[8192];

        Files.createFile(
                target,
                PosixFilePermissions.asFileAttribute(
                        PosixFilePermissions.fromString("rw-------")
                )
        );
        try (OutputStream outputStream = Files.newOutputStream(
                target,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytes += bytesRead;

                if (totalBytes > maximumBytes) {
                    throw new IOException("Uploaded file exceeds the maximum allowed size");
                }

                digest.update(buffer, 0, bytesRead);
                outputStream.write(buffer, 0, bytesRead);
            }

            if (totalBytes == 0) {
                throw new IOException("Uploaded file is empty");
            }

        } catch (IOException exception) {
            Files.deleteIfExists(target);
            throw exception;
        }

        return new StoredCvFile(
                storageKey,
                totalBytes,
                HexFormat.of().formatHex(digest.digest())
        );
    }

    @Override
    public long copyTo(
            String storageKey,
            OutputStream outputStream,
            long maximumBytes
    ) throws IOException {
        Path target = resolveStorageKey(storageKey);
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Stored CV could not be found");
        }

        Set<java.nio.file.OpenOption> options = Set.of(
                StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS
        );
        try (SeekableByteChannel channel = Files.newByteChannel(target, options);
             InputStream inputStream = Channels.newInputStream(channel)) {
            return StorageTransfer.copy(
                    inputStream,
                    outputStream,
                    maximumBytes
            );
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolveStorageKey(storageKey));
    }

    private Path resolveStorageKey(String storageKey) throws IOException {
        if (storageKey == null || !storageKey.matches("[a-f0-9-]{36}")) {
            throw new IOException("Invalid storage key");
        }

        Path target = storageDirectory.resolve(storageKey).normalize();

        if (!storageDirectory.equals(target.getParent())) {
            throw new IOException("Invalid storage path");
        }
        return target;
    }
}
