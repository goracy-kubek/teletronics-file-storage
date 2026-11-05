package com.teletronics.storage.integration;

import com.jayway.jsonpath.JsonPath;

import java.io.OutputStream;
import java.net.Socket;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.teletronics.storage.testutils.ConcurrentUtils.runConcurrently;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests of a file's life through the HTTP API: upload, listing, download and delete.
 * Each test uses its own random user, so tests do not affect each other.
 */
class FileLifecycleIT extends IntegrationTest {
    private static final String USER_ID_HEADER = "X-User-Id";

    @Test
    @DisplayName("Uploaded file appears in the owner's list with the metadata given at upload")
    void uploadedFileAppearsInOwnList() throws Exception {
        String user = "user-" + UUID.randomUUID();

        HttpResponse<String> uploaded = send(
                HttpRequest.newBuilder(uri("/api/files?filename=hello.txt&visibility=PRIVATE&tags=work"))
                        .header(USER_ID_HEADER, user)
                        .header("Content-Type", "application/octet-stream")
                        .POST(HttpRequest.BodyPublishers.ofByteArray("hello".getBytes(UTF_8))),
                HttpResponse.BodyHandlers.ofString());
        assertThat(uploaded.statusCode()).isEqualTo(201);
        String id = JsonPath.read(uploaded.body(), "$.id");

        HttpResponse<String> list = send(
                HttpRequest.newBuilder(uri("/api/files")).header(USER_ID_HEADER, user).GET(),
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
    @DisplayName("File is uploaded, listed and downloaded by its link; after delete it is gone and the link returns 404")
    void uploadFetchAndDelete() throws Exception {
        String user = "user-" + UUID.randomUUID();
        byte[] data = "hello".getBytes(UTF_8);

        // upload
        HttpResponse<String> uploaded = send(
                HttpRequest.newBuilder(uri("/api/files?filename=hello.txt&visibility=PRIVATE"))
                        .header(USER_ID_HEADER, user)
                        .header("Content-Type", "application/octet-stream")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(data)),
                HttpResponse.BodyHandlers.ofString());
        assertThat(uploaded.statusCode()).isEqualTo(201);
        String id = JsonPath.read(uploaded.body(), "$.id");
        String link = JsonPath.read(uploaded.body(), "$.downloadUrl");

        // fetch: appears in the own list
        HttpResponse<String> list = send(
                HttpRequest.newBuilder(uri("/api/files")).header(USER_ID_HEADER, user).GET(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<List<String>>read(list.body(), "$.content[*].id")).containsExactly(id);

        // fetch: content downloads by the link
        HttpResponse<byte[]> downloaded = send(
                HttpRequest.newBuilder(uri(link)).header(USER_ID_HEADER, user).GET(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(downloaded.statusCode()).isEqualTo(200);
        assertThat(downloaded.body()).isEqualTo(data);

        // delete
        HttpResponse<Void> deleted = send(
                HttpRequest.newBuilder(uri("/api/files/" + id)).header(USER_ID_HEADER, user).DELETE(),
                HttpResponse.BodyHandlers.discarding());
        assertThat(deleted.statusCode()).isEqualTo(204);

        // gone: not in the list, link no longer works
        HttpResponse<String> listAfter = send(
                HttpRequest.newBuilder(uri("/api/files")).header(USER_ID_HEADER, user).GET(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(JsonPath.<List<String>>read(listAfter.body(), "$.content")).isEmpty();

        HttpResponse<Void> downloadAfter = send(
                HttpRequest.newBuilder(uri(link)).header(USER_ID_HEADER, user).GET(),
                HttpResponse.BodyHandlers.discarding());
        assertThat(downloadAfter.statusCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("Parallel uploads with the same filename: one succeeds, the others get 409")
    void parallelUploadsWithSameFilenameCreateOnlyOneFile() throws Exception {
        int threads = 10;
        String user = "user-" + UUID.randomUUID();
        AtomicInteger sequence = new AtomicInteger();

        List<Integer> statuses = runConcurrently(threads, () -> {
            byte[] data = ("content-" + sequence.incrementAndGet()).getBytes(UTF_8);
            return send(
                    HttpRequest.newBuilder(uri("/api/files?filename=same.txt&visibility=PRIVATE"))
                            .header(USER_ID_HEADER, user)
                            .header("Content-Type", "application/octet-stream")
                            .POST(HttpRequest.BodyPublishers.ofByteArray(data)),
                    HttpResponse.BodyHandlers.discarding()).statusCode();
        });

        assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
        assertThat(statuses).filteredOn(status -> status == 409).hasSize(threads - 1);

        HttpResponse<String> list = send(
                HttpRequest.newBuilder(uri("/api/files")).header(USER_ID_HEADER, user).GET(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(JsonPath.<Integer>read(list.body(), "$.totalElements")).isEqualTo(1);
        assertThat(JsonPath.<String>read(list.body(), "$.content[0].filename")).isEqualTo("same.txt");
    }

    @Test
    @DisplayName("Parallel uploads with the same content: one succeeds, the others get 409")
    void parallelUploadsWithSameContentCreateOnlyOneFile() throws Exception {
        int threads = 10;
        String user = "user-" + UUID.randomUUID();
        byte[] data = "same content".getBytes(UTF_8);
        AtomicInteger sequence = new AtomicInteger();

        List<Integer> statuses = runConcurrently(threads, () -> {
            String filename = "copy-" + sequence.incrementAndGet() + ".txt";
            return send(
                    HttpRequest.newBuilder(uri("/api/files?filename=" + filename + "&visibility=PRIVATE"))
                            .header(USER_ID_HEADER, user)
                            .header("Content-Type", "application/octet-stream")
                            .POST(HttpRequest.BodyPublishers.ofByteArray(data)),
                    HttpResponse.BodyHandlers.discarding()).statusCode();
        });

        assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
        assertThat(statuses).filteredOn(status -> status == 409).hasSize(threads - 1);

        HttpResponse<String> list = send(
                HttpRequest.newBuilder(uri("/api/files")).header(USER_ID_HEADER, user).GET(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(JsonPath.<Integer>read(list.body(), "$.totalElements")).isEqualTo(1);
    }

    @Test
    @DisplayName("Public list has the PUBLIC files of all users and never the PRIVATE ones")
    void publicListHasPublicFilesOfAllUsers() throws Exception {
        String tag = "tag-" + UUID.randomUUID();
        String firstUser = "user-" + UUID.randomUUID();
        String secondUser = "user-" + UUID.randomUUID();

        // upload: two users each share a PUBLIC file, and the first user also keeps a PRIVATE one
        String firstPublic = upload(firstUser, "first-public.txt", "PUBLIC", tag, "first public");
        String secondPublic = upload(secondUser, "second-public.txt", "PUBLIC", tag, "second public");
        upload(firstUser, "first-private.txt", "PRIVATE", tag, "first private");

        // list the public files: no user is needed, and the unique tag keeps other tests' files out
        HttpResponse<String> list = send(
                HttpRequest.newBuilder(uri("/api/files/public?tag=" + tag)).GET(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(list.body(), "$.totalElements")).isEqualTo(2);
        assertThat(JsonPath.<List<String>>read(list.body(), "$.content[*].id"))
                .containsExactlyInAnyOrder(firstPublic, secondPublic);
        assertThat(JsonPath.<List<String>>read(list.body(), "$.content[*].visibility"))
                .containsOnly("PUBLIC");
    }

    @Test
    @DisplayName("Rename changes only the filename: the link still downloads the same content and the old name is free")
    void renameChangesOnlyTheFilename() throws Exception {
        String user = "user-" + UUID.randomUUID();

        // upload: the file to rename
        String uploaded = uploadAndGetBody(user, "old.txt", "PRIVATE", "work", "hello");
        String id = JsonPath.read(uploaded, "$.id");
        String link = JsonPath.read(uploaded, "$.downloadUrl");

        // rename the file by its id
        HttpResponse<String> renamed = rename(user, id, "new.txt");
        assertThat(renamed.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(renamed.body(), "$.id")).isEqualTo(id);
        assertThat(JsonPath.<String>read(renamed.body(), "$.filename")).isEqualTo("new.txt");
        assertThat(JsonPath.<String>read(renamed.body(), "$.downloadUrl")).isEqualTo(link);
        assertThat(JsonPath.<List<String>>read(renamed.body(), "$.tags")).containsExactly("work");

        // list the user's files: only the new name is shown
        HttpResponse<String> list = send(
                HttpRequest.newBuilder(uri("/api/files")).header(USER_ID_HEADER, user).GET(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(JsonPath.<List<String>>read(list.body(), "$.content[*].filename")).containsExactly("new.txt");

        // download: the same link still returns the same content
        HttpResponse<String> downloaded = send(
                HttpRequest.newBuilder(uri(link)).GET(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(downloaded.statusCode()).isEqualTo(200);
        assertThat(downloaded.body()).isEqualTo("hello");

        // upload again: the old filename is free for a new file
        upload(user, "old.txt", "PRIVATE", "work", "another content");
    }

    @Test
    @DisplayName("Rename to a filename of another file of the same user gives 409")
    void renameToTakenFilenameGivesConflict() throws Exception {
        String user = "user-" + UUID.randomUUID();
        String first = upload(user, "first.txt", "PRIVATE", "work", "first");
        upload(user, "second.txt", "PRIVATE", "work", "second");

        // rename: the first file takes the name of the second one
        HttpResponse<String> renamed = rename(user, first, "second.txt");

        assertThat(renamed.statusCode()).isEqualTo(409);
    }

    @Test
    @DisplayName("Rename of a file of another user gives 404 and leaves the file unchanged")
    void renameOfFileOfAnotherUserGivesNotFound() throws Exception {
        String owner = "user-" + UUID.randomUUID();
        String stranger = "user-" + UUID.randomUUID();
        String id = upload(owner, "mine.txt", "PRIVATE", "work", "mine");

        // rename: a different user tries to rename the file
        HttpResponse<String> renamed = rename(stranger, id, "stolen.txt");
        assertThat(renamed.statusCode()).isEqualTo(404);

        // list: the owner still sees the original name
        HttpResponse<String> list = send(
                HttpRequest.newBuilder(uri("/api/files")).header(USER_ID_HEADER, owner).GET(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(JsonPath.<List<String>>read(list.body(), "$.content[*].filename")).containsExactly("mine.txt");
    }

    @Test
    @DisplayName("Upload cut off midway gives 400, leaves no file and frees the filename")
    void interruptedUploadGivesBadRequestAndLeavesNoFile() throws Exception {
        String user = "user-" + UUID.randomUUID();

        // upload: the request promises 1,000,000 bytes, sends only 1,000 and then closes its sending side,
        // as a client that loses its connection (a raw socket, since HttpClient always sends the full body)
        String headers = "POST /api/files?filename=cut.txt&visibility=PRIVATE HTTP/1.1\r\n"
                + "Host: localhost:" + port + "\r\n"
                + "X-User-Id: " + user + "\r\n"
                + "Content-Type: application/octet-stream\r\n"
                + "Content-Length: 1000000\r\n"
                + "Connection: close\r\n\r\n";
        String response;
        try (Socket socket = new Socket("localhost", port)) {
            socket.setSoTimeout(30_000);
            OutputStream out = socket.getOutputStream();
            out.write(headers.getBytes(UTF_8));
            out.write(new byte[1_000]);
            out.flush();
            socket.shutdownOutput();
            response = new String(socket.getInputStream().readAllBytes(), UTF_8);
        }
        assertThat(response).startsWith("HTTP/1.1 400");

        // list the user's files: nothing is shown, the reservation was discarded
        HttpResponse<String> list = send(
                HttpRequest.newBuilder(uri("/api/files")).header(USER_ID_HEADER, user).GET(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(JsonPath.<List<String>>read(list.body(), "$.content")).isEmpty();

        // upload again: the same filename is free
        upload(user, "cut.txt", "PRIVATE", "work", "complete content");
    }

    private HttpResponse<String> rename(String user, String fileId, String newFilename) throws Exception {
        return send(
                HttpRequest.newBuilder(uri("/api/files/" + fileId))
                        .header(USER_ID_HEADER, user)
                        .header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(
                                "{\"filename\":\"" + newFilename + "\"}")),
                HttpResponse.BodyHandlers.ofString());
    }

    private String upload(String user, String filename, String visibility, String tag, String content)
            throws Exception {
        return JsonPath.read(uploadAndGetBody(user, filename, visibility, tag, content), "$.id");
    }

    private String uploadAndGetBody(String user, String filename, String visibility, String tag, String content)
            throws Exception {
        HttpResponse<String> uploaded = send(
                HttpRequest.newBuilder(uri("/api/files?filename=" + filename
                                + "&visibility=" + visibility + "&tags=" + tag))
                        .header(USER_ID_HEADER, user)
                        .header("Content-Type", "application/octet-stream")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(content.getBytes(UTF_8))),
                HttpResponse.BodyHandlers.ofString());
        assertThat(uploaded.statusCode()).isEqualTo(201);
        return uploaded.body();
    }

    private <T> HttpResponse<T> send(HttpRequest.Builder request, HttpResponse.BodyHandler<T> handler)
            throws Exception {
        return http.send(request.build(), handler);
    }
}