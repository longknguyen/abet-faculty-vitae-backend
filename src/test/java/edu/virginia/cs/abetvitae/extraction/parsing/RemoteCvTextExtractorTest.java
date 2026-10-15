package edu.virginia.cs.abetvitae.extraction.parsing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class RemoteCvTextExtractorTest {

    private static final String TOKEN = "0123456789abcdef0123456789abcdef";

    @TempDir
    private Path temporaryDirectory;

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void streamsTheFileToTheAuthenticatedWorker() throws IOException {
        byte[] fileContent = "safe staged content".getBytes(StandardCharsets.UTF_8);
        Path file = temporaryDirectory.resolve("upload.pdf");
        Files.write(file, fileContent);
        server = server(exchange -> {
            assertThat(exchange.getRequestHeaders().getFirst("Authorization"))
                    .isEqualTo("Bearer " + TOKEN);
            assertThat(exchange.getRequestBody().readAllBytes()).isEqualTo(fileContent);
            respond(
                    exchange,
                    200,
                    """
                    {"detectedMediaType":"application/pdf","text":"CV text","metadata":{}}
                    """);
        });

        ExtractedCvText result = extractor(Duration.ofSeconds(2)).extract(file, "faculty cv.pdf");

        assertThat(result.detectedMediaType()).isEqualTo("application/pdf");
        assertThat(result.text()).isEqualTo("CV text");
    }

    @Test
    void mapsAWorkerRejectionToTheSharedFailure() throws IOException {
        Path file = temporaryDirectory.resolve("upload.pdf");
        Files.writeString(file, "not a pdf", StandardCharsets.UTF_8);
        server = server(exchange -> respond(
                exchange,
                422,
                """
                {"failure":"UNSUPPORTED_MEDIA_TYPE","message":"Unsupported document"}
                """));

        assertThatThrownBy(() -> extractor(Duration.ofSeconds(2)).extract(file, "upload.pdf"))
                .isInstanceOfSatisfying(CvTextExtractionException.class, exception ->
                        assertThat(exception.getFailure())
                                .isEqualTo(CvTextExtractionFailure.UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    void enforcesTheBackendRequestDeadline() throws IOException {
        Path file = temporaryDirectory.resolve("upload.pdf");
        Files.writeString(file, "content", StandardCharsets.UTF_8);
        server = server(exchange -> {
            try {
                Thread.sleep(500L);
                respond(exchange, 200, "{}");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });

        assertThatThrownBy(() -> extractor(Duration.ofMillis(50)).extract(file, "upload.pdf"))
                .isInstanceOfSatisfying(CvTextExtractionException.class, exception ->
                        assertThat(exception.getFailure())
                                .isEqualTo(CvTextExtractionFailure.WORKER_TIMEOUT));
    }

    private RemoteCvTextExtractor extractor(Duration timeout) {
        CvParserWorkerProperties properties = new CvParserWorkerProperties(
                true,
                URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                TOKEN,
                Duration.ofSeconds(1),
                timeout,
                1_000_000L,
                10_000);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        return new RemoteCvTextExtractor(
                httpClient,
                new ParserWorkerRequestFactory(properties),
                new ParserWorkerResponseDecoder(properties, JsonMapper.builder().build()));
    }

    private static HttpServer server(ThrowingHandler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/internal/v1/extract", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        httpServer.start();
        return httpServer;
    }

    private static void respond(HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @FunctionalInterface
    private interface ThrowingHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
