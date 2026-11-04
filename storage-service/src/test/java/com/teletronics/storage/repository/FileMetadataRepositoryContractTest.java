package com.teletronics.storage.repository;

import com.teletronics.storage.domain.FileReservation;
import com.teletronics.storage.domain.FileStatus;
import com.teletronics.storage.domain.StoredFile;
import com.teletronics.storage.domain.Visibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract of {@link FileMetadataRepository#reserve}, independent of the implementation.
 * Every implementation gets a subclass that provides the repository under test.
 */
public abstract class FileMetadataRepositoryContractTest {

    /** The implementation under test. */
    protected abstract FileMetadataRepository repository();

    private String owner;

    @BeforeEach
    void newOwner() {
        owner = "user-" + UUID.randomUUID();
    }

    @Test
    void createsPendingRecord() {
        Instant before = Instant.now();

        StoredFile file = repository().reserve(
                new FileReservation(owner, "report.pdf", Visibility.PRIVATE, Set.of("work")));

        assertThat(file.id()).isNotBlank();
        assertThat(file.status()).isEqualTo(FileStatus.PENDING);
        assertThat(file.uploadDate()).isBetween(before, Instant.now());
    }
}