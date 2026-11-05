package com.teletronics.storage.integration;

import com.jayway.jsonpath.JsonPath;
import com.teletronics.storage.helpers.PatternInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
@DisplayName("Large file upload")
class LargeFileIT extends IntegrationTest {
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final int BUFFER_SIZE = 64 * 1024;

    private static final long SIZE = 2L * 1024 * 1024 * 1024 + 1;

    @Test
    @Timeout(value = 20, unit = TimeUnit.MINUTES)
    @DisplayName("File of more than 2 GB is streamed in, listed with its exact size and downloaded unchanged")
    void uploadsAndDownloadsFileLargerThanTwoGigabytes() throws Exception {
        String user = "user-" + UUID.randomUUID();
        MessageDigest sent = MessageDigest.getInstance("SHA-256");

        // upload: the body is generated on the fly and its SHA-256 is computed while it is sent
        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.fromPublisher(
                HttpRequest.BodyPublishers.ofInputStream(
                        () -> new DigestInputStream(new PatternInputStream(SIZE), sent)),
                SIZE);

        HttpResponse<String> uploaded = http.send(
                HttpRequest.newBuilder(uri("/api/files?filename=large.bin&visibility=PRIVATE"))
                        .header(USER_ID_HEADER, user)
                        .header("Content-Type", "application/octet-stream")
                        .timeout(Duration.ofMinutes(15))
                        .POST(body)
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(uploaded.statusCode()).isEqualTo(201);
        assertThat(JsonPath.<Number>read(uploaded.body(), "$.size").longValue()).isEqualTo(SIZE);
        String link = JsonPath.read(uploaded.body(), "$.downloadUrl");

        // list the user's files: the file appears once with its exact size
        HttpResponse<String> list = http.send(
                HttpRequest.newBuilder(uri("/api/files")).header(USER_ID_HEADER, user).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Number>read(list.body(), "$.totalElements").longValue()).isEqualTo(1);
        assertThat(JsonPath.<String>read(list.body(), "$.content[0].filename")).isEqualTo("large.bin");
        assertThat(JsonPath.<Number>read(list.body(), "$.content[0].size").longValue()).isEqualTo(SIZE);

        // download by the link: the content is read as a stream and compared with what was sent
        HttpResponse<InputStream> downloaded = http.send(
                HttpRequest.newBuilder(uri(link))
                        .header(USER_ID_HEADER, user)
                        .timeout(Duration.ofMinutes(15))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofInputStream());
        assertThat(downloaded.statusCode()).isEqualTo(200);
        assertThat(downloaded.headers().firstValueAsLong("Content-Length")).hasValue(SIZE);

        MessageDigest received = MessageDigest.getInstance("SHA-256");
        long receivedBytes = 0;
        try (InputStream in = downloaded.body()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                received.update(buffer, 0, read);
                receivedBytes += read;
            }
        }

        assertThat(receivedBytes).isEqualTo(SIZE);
        assertThat(HexFormat.of().formatHex(received.digest()))
                .isEqualTo(HexFormat.of().formatHex(sent.digest()));
    }
}
