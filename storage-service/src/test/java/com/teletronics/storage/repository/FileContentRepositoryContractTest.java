package com.teletronics.storage.repository;

import com.teletronics.storage.exception.ContentNotFoundException;
import com.teletronics.storage.helpers.FailingInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

public abstract class FileContentRepositoryContractTest {

    /** The implementation under test. */
    protected abstract FileContentRepository repository();

    /** Reads stored content back; not part of the interface yet, so each implementation provides it. */
    protected abstract byte[] read(String contentId);

    /** For delete method, also not in contract, but should be in storages */
    protected abstract boolean exists(String contentId);

    @Test
    void storesBytesUnchanged() throws Exception {
        byte[] data = randomBytes(10_000);

        String id = repository().store(new ByteArrayInputStream(data));

        assertThat(read(id)).isEqualTo(data);
    }

    @Test
    void storesContentLargerThanOneChunk() throws Exception {
        byte[] data = randomBytes(1024 * 1024);

        String id = repository().store(new ByteArrayInputStream(data));

        assertThat(read(id)).isEqualTo(data);
    }

    @Test
    void eachCallCreatesNewContent() throws Exception {
        byte[] data = randomBytes(100);

        String first = repository().store(new ByteArrayInputStream(data));
        String second = repository().store(new ByteArrayInputStream(data));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void deletesStoredContent() throws Exception {
        String id = repository().store(new ByteArrayInputStream(randomBytes(100)));
        assertThat(exists(id)).isTrue();

        repository().delete(id);

        assertThat(exists(id)).isFalse();
    }

    @Test
    void deleteDoesNotAffectOtherContent() throws Exception {
        byte[] kept = randomBytes(100);
        String keptId = repository().store(new ByteArrayInputStream(kept));
        String deletedId = repository().store(new ByteArrayInputStream(randomBytes(200)));

        repository().delete(deletedId);

        assertThat(read(keptId)).isEqualTo(kept);
    }

    @Test
    void deleteIsIdempotent() throws Exception {
        String id = repository().store(new ByteArrayInputStream(randomBytes(100)));
        repository().delete(id);

        repository().delete(id);

        assertThat(exists(id)).isFalse();
    }

    @Test
    void deleteOfUnknownIdDoesNothing() {
        FileContentRepository repo = repository();

        assertThatCode(() -> repo.delete(UUID.randomUUID().toString())).doesNotThrowAnyException();
    }

    @Test
    void opensStoredContent() throws Exception {
        byte[] data = randomBytes(10_000);
        String id = repository().store(new ByteArrayInputStream(data));

        try (InputStream in = repository().download(id)) {
            assertThat(in.readAllBytes()).isEqualTo(data);
        }
    }

    @Test
    void contentCanBeOpenedRepeatedly() throws Exception {
        byte[] data = randomBytes(1_000);
        String id = repository().store(new ByteArrayInputStream(data));

        assertThat(read(id)).isEqualTo(data);
        assertThat(read(id)).isEqualTo(data);
    }

    @Test
    void eachOpenReturnsIndependentStreamFromFirstByte() throws Exception {
        byte[] data = randomBytes(1024 * 1024);
        String id = repository().store(new ByteArrayInputStream(data));

        try (
                InputStream first = repository().download(id);
                InputStream second = repository().download(id)
        ) {
            first.readNBytes(1_000);

            assertThat(second.readAllBytes()).isEqualTo(data);
        }
    }

    @Test
    void openDoesNotAffectOtherContent() throws Exception {
        byte[] first = randomBytes(100);
        byte[] second = randomBytes(200);
        String firstId = repository().store(new ByteArrayInputStream(first));
        String secondId = repository().store(new ByteArrayInputStream(second));

        assertThat(read(secondId)).isEqualTo(second);
        assertThat(read(firstId)).isEqualTo(first);
    }

    @Test
    @SuppressWarnings("resource")
    void openOfDeletedContentFails() throws Exception {
        String id = repository().store(new ByteArrayInputStream(randomBytes(100)));
        repository().delete(id);
        FileContentRepository repo = repository();

        assertThatThrownBy(() -> repo.download(id))
                .isInstanceOf(ContentNotFoundException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"000000000000000000000000", "not-an-id", ""})
    @SuppressWarnings("resource")
    void openOfUnknownIdFails(String unknownId) {
        FileContentRepository repo = repository();

        assertThatThrownBy(() -> repo.download(unknownId))
                .isInstanceOf(ContentNotFoundException.class)
                .hasMessageContaining(unknownId);
    }

    @Test
    @SuppressWarnings("resource")
    void interruptedStreamThrowsIOException() {
        FileContentRepository repo = repository();
        InputStream failing = new FailingInputStream(1024 * 1024);

        assertThatThrownBy(() -> repo.store(failing))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Simulated client disconnect");
    }

    private static byte[] randomBytes(int size) {
        byte[] data = new byte[size];
        new Random(size).nextBytes(data);
        return data;
    }
}