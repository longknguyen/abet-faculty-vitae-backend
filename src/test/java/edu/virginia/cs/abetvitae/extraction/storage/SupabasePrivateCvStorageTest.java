package edu.virginia.cs.abetvitae.extraction.storage;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SupabasePrivateCvStorageTest {

    @Test
    void uploadsFileSuccessfully() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200));

            String secretKey = "test-secret";
            String bucketName = "faculty-cvs";
            byte[] fileBytes = "test CV content".getBytes(StandardCharsets.UTF_8);

            SupabasePrivateCvStorage storage =
                    new SupabasePrivateCvStorage(
                            server.url("/").toString(),
                            secretKey,
                            bucketName,
                            HttpClient.newHttpClient()
                    );

            StoredCvFile storedFile = storage.store(
                    new ByteArrayInputStream(fileBytes),
                    1024,
                    "application/pdf"
            );

            RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

            assertNotNull(request);
            assertEquals("POST", request.getMethod());
            assertEquals(secretKey, request.getHeader("apikey"));
            assertEquals(
                    "Bearer " + secretKey,
                    request.getHeader("Authorization")
            );
            assertEquals(
                    "application/pdf",
                    request.getHeader("Content-Type")
            );
            assertEquals(
                    "false",
                    request.getHeader("x-upsert")
            );
            assertArrayEquals(fileBytes, request.getBody().readByteArray());

            assertDoesNotThrow(
                    () -> UUID.fromString(storedFile.storageKey())
            );
            assertEquals(fileBytes.length, storedFile.fileSizeBytes());

            String expectedHash = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(fileBytes)
            );

            assertEquals(expectedHash, storedFile.sha256());
        }
    }

    @Test
    void deletesFileSuccessfully() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200));

            String storageKey = UUID.randomUUID().toString();

            SupabasePrivateCvStorage storage =
                    new SupabasePrivateCvStorage(
                            server.url("/").toString(),
                            "test-secret",
                            "faculty-cvs",
                            HttpClient.newHttpClient()
                    );

            assertDoesNotThrow(() -> storage.delete(storageKey));

            RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

            assertNotNull(request);
            assertEquals("DELETE", request.getMethod());
            assertEquals(
                    "test-secret",
                    request.getHeader("apikey")
            );
            assertEquals(
                    "Bearer test-secret",
                    request.getHeader("Authorization")
            );
            assertTrue(request.getPath().startsWith(
                    "/storage/v1/object/faculty-cvs/"
            ));
            assertTrue(request.getPath().endsWith(storageKey));
        }
    }

    @Test
    void throwsExceptionWhenSupabaseUploadFails() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(500));

            SupabasePrivateCvStorage storage =
                    new SupabasePrivateCvStorage(
                            server.url("/").toString(),
                            "test-secret",
                            "faculty-cvs",
                            HttpClient.newHttpClient()
                    );

            byte[] fileBytes = "test CV content".getBytes(StandardCharsets.UTF_8);

            assertThrows(
                    IOException.class,
                    () -> storage.store(
                            new ByteArrayInputStream(fileBytes),
                            1024,
                            "application/pdf"
                    )
            );
        }
    }
}