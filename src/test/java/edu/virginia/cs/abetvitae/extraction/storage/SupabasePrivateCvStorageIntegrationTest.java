
package edu.virginia.cs.abetvitae.extraction.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.ByteArrayInputStream;
import java.net.http.HttpClient;
import java.time.Duration;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SupabasePrivateCvStorageIntegrationTest {

    @Test
    @EnabledIfEnvironmentVariable(
            named = "RUN_SUPABASE_INTEGRATION_TESTS",
            matches = "true"
    )
    void uploadsAndDeletesFileInSupabase() throws Exception {

        String supabaseUrl = System.getenv("SUPABASE_URL");
        String secretKey = System.getenv("SUPABASE_SECRET_KEY");
        String bucketName = System.getenv("SUPABASE_BUCKET");

        assertNotNull(supabaseUrl);
        assertNotNull(secretKey);
        assertNotNull(bucketName);

        SupabasePrivateCvStorage storage =
                new SupabasePrivateCvStorage(
                        supabaseUrl,
                        secretKey,
                        bucketName,
                        HttpClient.newHttpClient(),
                        Duration.ofSeconds(15)
                );

        byte[] fileBytes =
                "Supabase storage integration test"
                        .getBytes(StandardCharsets.UTF_8);

        StoredCvFile storedFile = storage.store(
                new ByteArrayInputStream(fileBytes),
                1024,
                "application/pdf"
        );

        try {
            assertNotNull(storedFile.storageKey());
            assertEquals(fileBytes.length, storedFile.fileSizeBytes());
            assertNotNull(storedFile.sha256());
            assertEquals(64, storedFile.sha256().length());
        } finally {
            storage.delete(storedFile.storageKey());
        }
    }
}
