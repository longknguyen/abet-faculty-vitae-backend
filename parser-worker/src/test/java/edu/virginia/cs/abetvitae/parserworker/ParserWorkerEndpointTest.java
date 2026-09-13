package edu.virginia.cs.abetvitae.parserworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        classes = ParserWorkerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "parser-worker.token=0123456789abcdef0123456789abcdef")
class ParserWorkerEndpointTest {

    private static final String TOKEN = "0123456789abcdef0123456789abcdef";

    @LocalServerPort
    private int port;

    @Test
    void rejectsRequestsWithoutThePrivateToken() throws IOException, InterruptedException {
        HttpResponse<String> response = client().send(
                request(null), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).doesNotContain("Tika", "Exception");
    }

    @Test
    void returnsAStableFailureWithoutLeakingInternals() throws IOException, InterruptedException {
        HttpResponse<String> response = client().send(
                request(TOKEN), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(422);
        assertThat(response.body()).contains("UNSUPPORTED_MEDIA_TYPE");
        assertThat(response.body()).doesNotContain("Tika", "Exception", "/tmp");
    }

    private HttpClient client() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
    }

    private HttpRequest request(String token) {
        String filename = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("cv.pdf".getBytes(StandardCharsets.UTF_8));
        HttpRequest.Builder request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + "/internal/v1/extract"))
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", "application/octet-stream")
                .header(ParserWorkerController.ORIGINAL_FILENAME_HEADER, filename)
                .POST(HttpRequest.BodyPublishers.ofString("MZ"));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return request.build();
    }
}
