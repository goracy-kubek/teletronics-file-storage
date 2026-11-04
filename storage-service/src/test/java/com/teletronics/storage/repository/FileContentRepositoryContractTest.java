package com.teletronics.storage.repository;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
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

    private static byte[] randomBytes(int size) {
        byte[] data = new byte[size];
        new Random(size).nextBytes(data);
        return data;
    }
}