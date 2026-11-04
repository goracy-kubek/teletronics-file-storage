package com.teletronics.storage.integration;

import com.jayway.jsonpath.JsonPath;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class FileLifecycleIT extends IntegrationTest {

    @Test
    void uploadedFileAppearsInOwnList() throws Exception {
        String user = "user-" + UUID.randomUUID();

        HttpResponse<String> uploaded = send(
                HttpRequest.newBuilder(uri("/api/files?filename=hello.txt&visibility=PRIVATE&tags=work"))
                        .header("X-User-Id", user)
                        .header("Content-Type", "application/octet-stream")
                        .POST(HttpRequest.BodyPublishers.ofByteArray("hello".getBytes(UTF_8))),
                HttpResponse.BodyHandlers.ofString());
        assertThat(uploaded.statusCode()).isEqualTo(201);
        String id = JsonPath.read(uploaded.body(), "$.id");

        HttpResponse<String> list = send(
                HttpRequest.newBuilder(uri("/api/files")).header("X-User-Id", user).GET(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(list.body(), "$.totalElements")).isEqualTo(1);
        assertThat(JsonPath.<List<String>>read(list.body(), "$.content[*].id")).containsExactly(id);
        assertThat(JsonPath.<String>read(list.body(), "$.content[0].filename")).isEqualTo("hello.txt");
        assertThat(JsonPath.<String>read(list.body(), "$.content[0].visibility")).isEqualTo("PRIVATE");
        assertThat(JsonPath.<List<String>>read(list.body(), "$.content[0].tags")).containsExactly("work");
        assertThat(JsonPath.<Integer>read(list.body(), "$.content[0].size")).isEqualTo(5);
        assertThat(JsonPath.<String>read(list.body(), "$.content[0].downloadUrl")).startsWith("/download/");
    }

    @Test
//    @Disabled("Not implemented")
    void uploadFetchAndDelete() throws Exception {
        String user = "user-" + UUID.randomUUID();
        byte[] data = "hello".getBytes(UTF_8);

        // upload
        HttpResponse<String> uploaded = send(
                HttpRequest.newBuilder(uri("/api/files?filename=hello.txt&visibility=PRIVATE"))
                        .header("X-User-Id", user)
                        .header("Content-Type", "application/octet-stream")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(data)),
                HttpResponse.BodyHandlers.ofString());
        assertThat(uploaded.statusCode()).isEqualTo(201);
        String id = JsonPath.read(uploaded.body(), "$.id");
        String link = JsonPath.read(uploaded.body(), "$.downloadUrl");

        // fetch: appears in the own list
        HttpResponse<String> list = send(
                HttpRequest.newBuilder(uri("/api/files")).header("X-User-Id", user).GET(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<List<String>>read(list.body(), "$.content[*].id")).containsExactly(id);

        // fetch: content downloads by the link
        HttpResponse<byte[]> downloaded = send(
                HttpRequest.newBuilder(uri(link)).header("X-User-Id", user).GET(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(downloaded.statusCode()).isEqualTo(200);
        assertThat(downloaded.body()).isEqualTo(data);

        // delete
        HttpResponse<Void> deleted = send(
                HttpRequest.newBuilder(uri("/api/files/" + id)).header("X-User-Id", user).DELETE(),
                HttpResponse.BodyHandlers.discarding());
        assertThat(deleted.statusCode()).isEqualTo(204);

        // gone: not in the list, link no longer works
        HttpResponse<String> listAfter = send(
                HttpRequest.newBuilder(uri("/api/files")).header("X-User-Id", user).GET(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(JsonPath.<List<String>>read(listAfter.body(), "$.content")).isEmpty();

        HttpResponse<Void> downloadAfter = send(
                HttpRequest.newBuilder(uri(link)).header("X-User-Id", user).GET(),
                HttpResponse.BodyHandlers.discarding());
        assertThat(downloadAfter.statusCode()).isEqualTo(404);
    }

    private <T> HttpResponse<T> send(HttpRequest.Builder request, HttpResponse.BodyHandler<T> handler)
            throws Exception {
        return http.send(request.build(), handler);
    }
}