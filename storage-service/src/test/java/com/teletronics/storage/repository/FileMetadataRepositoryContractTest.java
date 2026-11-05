package com.teletronics.storage.repository;

import com.teletronics.storage.domain.*;
import com.teletronics.storage.exception.DuplicateFileException;
import com.teletronics.storage.exception.OwnedFileNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.teletronics.storage.testutils.ConcurrentUtils.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("File metadata repository contract")
public abstract class FileMetadataRepositoryContractTest {

    /** The implementation under test. */
    protected abstract FileMetadataRepository repository();

    /** Number of stored records of the owner; not part of the interface, so each implementation provides it. */
    protected abstract long countRecords(String ownerId);

    private String owner;

    @BeforeEach
    void newOwner() {
        owner = "user-" + UUID.randomUUID();
    }

    @Test
    @DisplayName("Reserve rejects a filename held by a PENDING record")
    void rejectsFilenameOfPendingRecord() {
        FileMetadataRepository repository = repository();
        repository.reserve(reservation("report.pdf"));
        FileReservation duplicate = reservation("report.pdf");

        assertThatThrownBy(() -> repository.reserve(duplicate))
                .isInstanceOf(DuplicateFileException.class)
                .hasMessageContaining("filename already exists");

        assertThat(countRecords(owner)).isEqualTo(1);
    }

    @Test
    @DisplayName("Reserve allows the same filename for different owners")
    void allowsSameFilenameForDifferentOwners() {
        String otherOwner = "user-" + UUID.randomUUID();

        repository().reserve(reservation("report.pdf"));
        StoredFile other = repository().reserve(
                new FileReservation(otherOwner, "report.pdf", Visibility.PUBLIC, Set.of()));

        assertThat(other.ownerId()).isEqualTo(otherOwner);
        assertThat(countRecords(owner)).isEqualTo(1);
        assertThat(countRecords(otherOwner)).isEqualTo(1);
    }

    @Test
    @DisplayName("Reserve creates a PENDING record")
    void createsPendingRecord() {
        Instant before = Instant.now();

        StoredFile file = repository().reserve(
                new FileReservation(owner, "report.pdf", Visibility.PRIVATE, Set.of("work", "2026")));

        assertThat(file.id()).isNotBlank();
        assertThat(file.ownerId()).isEqualTo(owner);
        assertThat(file.filename()).isEqualTo("report.pdf");
        assertThat(file.visibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(file.tags()).containsExactlyInAnyOrder("work", "2026");
        assertThat(file.status()).isEqualTo(FileStatus.PENDING);
        assertThat(file.uploadDate()).isBetween(before, Instant.now());
        assertThat(file.sha256()).isNull();
        assertThat(file.downloadToken()).isNull();
        assertThat(file.contentId()).isNull();
    }

    @Test
    @DisplayName("Reserve turns missing tags into an empty set")
    void emptyTagsBecomeEmptySet() {
        StoredFile file = repository().reserve(
                new FileReservation(owner, "no-tags.txt", Visibility.PUBLIC, Set.of()));

        assertThat(file.tags()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Reserve rejects a filename held by a READY record")
    void rejectsFilenameOfReadyRecord() {
        FileMetadataRepository repository = repository();
        StoredFile existing = repository.reserve(reservation("report.pdf"));
        repository.complete(existing.id(),
                new StoredContent("content-" + owner, "sha-" + owner, 10, "text/plain"),
                "token-" + owner);
        FileReservation duplicate = reservation("report.pdf");

        assertThatThrownBy(() -> repository.reserve(duplicate))
                .isInstanceOf(DuplicateFileException.class)
                .hasMessageContaining("filename already exists");

        assertThat(countRecords(owner)).isEqualTo(1);
    }

    @Test
    @DisplayName("Filenames are case-sensitive")
    void filenamesAreCaseSensitive() {
        repository().reserve(reservation("report.pdf"));

        StoredFile second = repository().reserve(reservation("Report.pdf"));

        assertThat(second.filename()).isEqualTo("Report.pdf");
        assertThat(countRecords(owner)).isEqualTo(2);
    }

    @Test
    @DisplayName("PENDING records of one owner with different filenames do not conflict")
    void pendingRecordsDoNotConflict() {
        repository().reserve(reservation("first.txt"));
        repository().reserve(reservation("second.txt"));

        assertThat(countRecords(owner)).isEqualTo(2);
    }

    @Test
    @DisplayName("Concurrent reservations of the same filename: only one succeeds")
    void concurrentReservationsWithSameFilename() throws Exception {
        int threads = 10;
        FileMetadataRepository repository = repository();
        FileReservation reservation = reservation("same.txt");

        List<Boolean> succeeded = runConcurrently(threads, () -> {
            try {
                repository.reserve(reservation);
                return true;
            } catch (DuplicateFileException _) {
                return false;
            }
        });

        assertThat(succeeded).filteredOn(s -> s).hasSize(1);
        assertThat(succeeded).filteredOn(s -> !s).hasSize(threads - 1);
        assertThat(countRecords(owner)).isEqualTo(1);
    }

    @Test
    @DisplayName("Complete turns a PENDING record into a READY one")
    void completesPendingRecord() {
        StoredFile pending = repository().reserve(new FileReservation(
                owner,
                "report.pdf",
                Visibility.PRIVATE,
                Set.of("work"))
        );

        StoredContent content = new StoredContent(
                "content-" + owner,
                "sha-" + owner,
                1024,
                "application/pdf"
        );

        StoredFile completed = repository().complete(pending.id(), content, "token-" + owner);

        assertThat(completed.status()).isEqualTo(FileStatus.READY);
        assertThat(completed.contentId()).isEqualTo(content.contentId());
        assertThat(completed.sha256()).isEqualTo(content.sha256());
        assertThat(completed.size()).isEqualTo(1024);
        assertThat(completed.contentType()).isEqualTo("application/pdf");
        assertThat(completed.downloadToken()).isEqualTo("token-" + owner);

        assertThat(completed.id()).isEqualTo(pending.id());
        assertThat(completed.ownerId()).isEqualTo(owner);
        assertThat(completed.filename()).isEqualTo("report.pdf");
        assertThat(completed.visibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(completed.tags()).containsExactly("work");
        assertThat(completed.uploadDate()).isEqualTo(pending.uploadDate().truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    @DisplayName("Complete rejects the same content for the same owner")
    void rejectsSameContentForSameOwner() {
        StoredFile first = repository().reserve(reservation("first.txt"));
        StoredFile second = repository().reserve(reservation("second.txt"));
        repository().complete(first.id(), content("same-sha"), "token-1-" + owner);

        FileMetadataRepository repo = repository();
        StoredContent duplicate = content("same-sha");
        String secondId = second.id();

        assertThatThrownBy(() -> repo.complete(secondId, duplicate, "token-2-" + owner))
                .isInstanceOf(DuplicateFileException.class)
                .hasMessageContaining("same content already uploaded");
    }

    @Test
    @DisplayName("Rejected complete leaves the record PENDING and unchanged")
    void leavesRecordUntouchedWhenRejected() {
        StoredFile first = repository().reserve(reservation("first.txt"));
        StoredFile second = repository().reserve(reservation("second.txt"));
        repository().complete(first.id(), content("same-sha"), "token-1-" + owner);

        FileMetadataRepository repo = repository();
        StoredContent duplicate = content("same-sha");
        String secondId = second.id();
        assertThatThrownBy(() -> repo.complete(secondId, duplicate, "token-2-" + owner))
                .isInstanceOf(DuplicateFileException.class);

        StoredFile completed = repository().complete(second.id(), content("other-sha"), "token-2-" + owner);

        assertThat(completed.status()).isEqualTo(FileStatus.READY);
        assertThat(completed.sha256()).isEqualTo("other-sha");
    }

    @Test
    @DisplayName("Complete allows the same content for different owners")
    void allowsSameContentForDifferentOwners() {
        String otherOwner = "user-" + UUID.randomUUID();
        StoredFile mine = repository().reserve(reservation("report.pdf"));
        StoredFile theirs = repository().reserve(
                new FileReservation(otherOwner, "report.pdf", Visibility.PUBLIC, Set.of()));

        StoredFile myCompleted = repository().complete(mine.id(), content("same-sha"), "token-1-" + owner);
        StoredFile theirCompleted = repository().complete(theirs.id(), content("same-sha"), "token-2-" + owner);

        assertThat(myCompleted.status()).isEqualTo(FileStatus.READY);
        assertThat(theirCompleted.status()).isEqualTo(FileStatus.READY);
    }

    @Test
    @DisplayName("Complete fails for an unknown id")
    void failsForUnknownId() {
        FileMetadataRepository repo = repository();
        StoredContent stored = content("some-sha");

        var notExistedFile = UUID.randomUUID().toString();

        assertThatThrownBy(() -> repo.complete(notExistedFile, stored, "token-" + owner))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("A record cannot be completed twice")
    void cannotCompleteTwice() {
        StoredFile file = repository().reserve(reservation("report.pdf"));
        repository().complete(file.id(), content("first-sha"), "token-1-" + owner);

        FileMetadataRepository repo = repository();
        String fileId = file.id();
        StoredContent secondContent = content("second-sha");
        assertThatThrownBy(() -> repo.complete(fileId, secondContent, "token-2-" + owner))
                .isInstanceOf(IllegalStateException.class);

        StoredFile other = repository().reserve(reservation("other.txt"));
        StoredFile otherCompleted = repository().complete(other.id(), content("second-sha"), "token-3-" + owner);
        assertThat(otherCompleted.sha256()).isEqualTo("second-sha");
    }

    @Test
    @DisplayName("Concurrent completions with the same content: only one succeeds")
    void concurrentCompletionsWithSameContent() throws Exception {
        int threads = 10;
        Queue<String> pendingIds = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < threads; i++) {
            pendingIds.add(repository().reserve(reservation("copy-" + i + ".txt")).id());
        }

        List<Boolean> succeeded = runConcurrently(threads, () -> {
            String id = pendingIds.poll();
            try {
                repository().complete(id, content("same-sha"), "token-" + id);
                return true;
            } catch (DuplicateFileException _) {
                return false;
            }
        });

        assertThat(succeeded).filteredOn(s -> s).hasSize(1);
        assertThat(succeeded).filteredOn(s -> !s).hasSize(threads - 1);
    }

    @Test
    @DisplayName("Discard removes a PENDING record and frees its filename")
    void discardsPendingRecordAndFreesFilename() {
        StoredFile pending = repository().reserve(reservation("report.pdf"));
        assertThat(countRecords(owner)).isEqualTo(1);

        repository().discard(pending.id());

        assertThat(countRecords(owner)).isZero();
        StoredFile again = repository().reserve(reservation("report.pdf"));
        assertThat(again.filename()).isEqualTo("report.pdf");
    }

    @Test
    @DisplayName("Discard does not remove a READY record")
    void doesNotDiscardReadyRecord() {
        StoredFile file = repository().reserve(reservation("report.pdf"));
        repository().complete(file.id(), content("sha-" + owner), "token-" + owner);
        assertThat(countRecords(owner)).isEqualTo(1);

        repository().discard(file.id());

        assertThat(countRecords(owner)).isEqualTo(1);
    }

    @Test
    @DisplayName("Discarding an unknown id does nothing")
    void discardOfUnknownIdDoesNothing() {
        repository().reserve(reservation("report.pdf"));

        repository().discard(UUID.randomUUID().toString());

        assertThat(countRecords(owner)).isEqualTo(1);
    }

    @Test
    @DisplayName("Discarding twice is allowed")
    void discardIsIdempotent() {
        StoredFile pending = repository().reserve(reservation("report.pdf"));
        repository().discard(pending.id());
        assertThat(countRecords(owner)).isZero();

        repository().discard(pending.id());

        assertThat(countRecords(owner)).isZero();
    }

    @Test
    @DisplayName("Delete removes a READY file of the owner and returns it")
    void deletesReadyFileOfOwnerAndReturnsIt() {
        StoredFile file = create("report.pdf");
        assertThat(countRecords(owner)).isEqualTo(1);

        StoredFile deleted = repository().deleteOwned(file.id(), owner);

        assertThat(deleted.id()).isEqualTo(file.id());
        assertThat(deleted.contentId()).isEqualTo(file.contentId());
        assertThat(countRecords(owner)).isZero();
    }

    @Test
    @DisplayName("Delete does not remove a file of another owner")
    void doesNotDeleteFileOfAnotherOwner() {
        StoredFile file = create("report.pdf");
        FileMetadataRepository repo = repository();
        String fileId = file.id();
        String other = "user-" + UUID.randomUUID();

        assertThatThrownBy(() -> repo.deleteOwned(fileId, other))
                .isInstanceOf(OwnedFileNotFoundException.class)
                .hasMessage("File " + fileId + " not found among files of user " + other);

        assertThat(countRecords(owner)).isEqualTo(1);
    }

    @Test
    @DisplayName("Delete does not remove a PENDING record")
    void doesNotDeletePendingRecord() {
        StoredFile pending = repository().reserve(reservation("report.pdf"));
        FileMetadataRepository repo = repository();
        String pendingId = pending.id();

        assertThatThrownBy(() -> repo.deleteOwned(pendingId, owner))
                .isInstanceOf(OwnedFileNotFoundException.class)
                .hasMessage("File " + pendingId + " not found among files of user " + owner);

        assertThat(countRecords(owner)).isEqualTo(1);
    }

    @Test
    @DisplayName("Delete fails for an unknown id")
    void deleteOfUnknownIdFails() {
        create("report.pdf");
        FileMetadataRepository repo = repository();
        String unknownId = UUID.randomUUID().toString();

        assertThatThrownBy(() -> repo.deleteOwned(unknownId, owner))
                .isInstanceOf(OwnedFileNotFoundException.class)
                .hasMessage("File " + unknownId + " not found among files of user " + owner);

        assertThat(countRecords(owner)).isEqualTo(1);
    }

    @Test
    @DisplayName("Deleted file frees its filename and content")
    void deletedFileFreesFilenameAndContent() {
        StoredFile file = repository().reserve(reservation("report.pdf"));
        repository().complete(file.id(), content("same-sha"), "token-1-" + owner);
        repository().deleteOwned(file.id(), owner);

        StoredFile again = repository().reserve(reservation("report.pdf"));
        StoredFile completed = repository().complete(again.id(), content("same-sha"), "token-2-" + owner);

        assertThat(completed.status()).isEqualTo(FileStatus.READY);
    }

    @Test
    @DisplayName("Deleting the same file twice fails the second time")
    void secondDeleteFails() {
        StoredFile file = create("report.pdf");
        FileMetadataRepository repo = repository();
        String fileId = file.id();
        repo.deleteOwned(fileId, owner);

        assertThatThrownBy(() -> repo.deleteOwned(fileId, owner))
                .isInstanceOf(OwnedFileNotFoundException.class)
                .hasMessage("File " + fileId + " not found among files of user " + owner);
    }

    @Test
    @DisplayName("Concurrent deletes of the same file: only one succeeds")
    void concurrentDeletesOfSameFileSucceedOnce() throws Exception {
        int threads = 10;
        FileMetadataRepository repository = repository();
        StoredFile file = create("test.pdf");
        String fileId = file.id();

        List<Boolean> deleted = runConcurrently(threads, () -> {
            try {
                repository.deleteOwned(fileId, owner);
                return true;
            } catch (OwnedFileNotFoundException _) {
                return false;
            }
        });

        assertThat(deleted).filteredOn(d -> d).hasSize(1);
        assertThat(deleted).filteredOn(d -> !d).hasSize(threads - 1);
        assertThat(countRecords(owner)).isZero();
    }

    @Test
    @DisplayName("Rename changes only the filename of a READY file")
    void renamesReadyFileAndKeepsEverythingElse() {
        StoredFile file = create("old.pdf");

        StoredFile renamed = repository().rename(file.id(), owner, "new.pdf");

        assertThat(renamed.filename()).isEqualTo("new.pdf");
        assertThat(renamed.id()).isEqualTo(file.id());
        assertThat(renamed.ownerId()).isEqualTo(owner);
        assertThat(renamed.status()).isEqualTo(FileStatus.READY);
        assertThat(renamed.contentId()).isEqualTo(file.contentId());
        assertThat(renamed.sha256()).isEqualTo(file.sha256());
        assertThat(renamed.downloadToken()).isEqualTo(file.downloadToken());
        assertThat(countRecords(owner)).isEqualTo(1);
    }

    @Test
    @DisplayName("Rename frees the old filename and takes the new one")
    void renameFreesOldFilenameAndTakesNewOne() {
        StoredFile file = create("old.pdf");
        repository().rename(file.id(), owner, "new.pdf");

        StoredFile reusedOld = repository().reserve(reservation("old.pdf"));
        FileMetadataRepository repo = repository();
        FileReservation takenNew = reservation("new.pdf");

        assertThat(reusedOld.filename()).isEqualTo("old.pdf");
        assertThatThrownBy(() -> repo.reserve(takenNew))
                .isInstanceOf(DuplicateFileException.class);
    }

    @Test
    @DisplayName("Rename to a filename of another file of the same owner fails")
    void renameToFilenameOfAnotherFileOfSameOwnerFails() {
        StoredFile first = create("first.pdf");
        create("second.pdf");
        FileMetadataRepository repo = repository();
        String firstId = first.id();

        assertThatThrownBy(() -> repo.rename(firstId, owner, "second.pdf"))
                .isInstanceOf(DuplicateFileException.class)
                .hasMessage("filename already exists");

        // the failed rename changed nothing: the old name is still taken
        FileReservation oldName = reservation("first.pdf");
        assertThatThrownBy(() -> repo.reserve(oldName))
                .isInstanceOf(DuplicateFileException.class);
    }

    @Test
    @DisplayName("Rename to a filename used by another owner is allowed")
    void renameToFilenameUsedByAnotherOwnerIsAllowed() {
        String otherOwner = "user-" + UUID.randomUUID();
        repository().reserve(new FileReservation(otherOwner, "shared.pdf", Visibility.PUBLIC, Set.of()));
        StoredFile mine = create("mine.pdf");

        StoredFile renamed = repository().rename(mine.id(), owner, "shared.pdf");

        assertThat(renamed.filename()).isEqualTo("shared.pdf");
    }

    @Test
    @DisplayName("Rename to the file's own current filename succeeds")
    void renameToOwnCurrentFilenameSucceeds() {
        StoredFile file = create("same.pdf");

        StoredFile renamed = repository().rename(file.id(), owner, "same.pdf");

        assertThat(renamed.filename()).isEqualTo("same.pdf");
    }

    @Test
    @DisplayName("Rename does not change a file of another owner")
    void doesNotRenameFileOfAnotherOwner() {
        StoredFile file = create("report.pdf");
        FileMetadataRepository repo = repository();
        String fileId = file.id();
        String other = "user-" + UUID.randomUUID();

        assertThatThrownBy(() -> repo.rename(fileId, other, "stolen.pdf"))
                .isInstanceOf(OwnedFileNotFoundException.class)
                .hasMessage("File " + fileId + " not found among files of user " + other);

        // still reachable under the old name by the real owner
        assertThat(repo.rename(fileId, owner, "still-mine.pdf").filename()).isEqualTo("still-mine.pdf");
    }

    @Test
    @DisplayName("Rename does not change a PENDING record")
    void doesNotRenamePendingRecord() {
        StoredFile pending = repository().reserve(reservation("report.pdf"));
        FileMetadataRepository repo = repository();
        String pendingId = pending.id();

        assertThatThrownBy(() -> repo.rename(pendingId, owner, "other.pdf"))
                .isInstanceOf(OwnedFileNotFoundException.class)
                .hasMessage("File " + pendingId + " not found among files of user " + owner);
    }

    @Test
    @DisplayName("Rename fails for an unknown id")
    void renameOfUnknownIdFails() {
        create("report.pdf");
        FileMetadataRepository repo = repository();
        String unknownId = UUID.randomUUID().toString();

        assertThatThrownBy(() -> repo.rename(unknownId, owner, "other.pdf"))
                .isInstanceOf(OwnedFileNotFoundException.class)
                .hasMessage("File " + unknownId + " not found among files of user " + owner);
    }

    @Test
    @DisplayName("Concurrent renames to the same filename: only one succeeds")
    void concurrentRenamesToSameFilenameSucceedOnce() throws Exception {
        int threads = 10;
        FileMetadataRepository repository = repository();
        Queue<String> ids = IntStream.range(0, threads)
                .mapToObj(i -> create("copy-" + i + ".txt").id())
                .collect(Collectors.toCollection(ConcurrentLinkedQueue::new));

        List<Boolean> renamed = runConcurrently(threads, () -> {
            try {
                repository.rename(ids.poll(), owner, "target.txt");
                return true;
            } catch (DuplicateFileException _) {
                return false;
            }
        });

        assertThat(renamed).filteredOn(r -> r).hasSize(1);
        assertThat(renamed).filteredOn(r -> !r).hasSize(threads - 1);
    }

    private StoredFile create(String filename) {
        StoredFile pending = repository().reserve(reservation(filename));
        String unique = UUID.randomUUID().toString();
        return repository().complete(pending.id(), content("sha-" + unique), "token-" + unique);
    }

    private StoredContent content(String sha256) {
        return new StoredContent("content-" + UUID.randomUUID(), sha256, 10, "text/plain");
    }

    private FileReservation reservation(String filename) {
        return new FileReservation(owner, filename, Visibility.PUBLIC, Set.of());
    }
}