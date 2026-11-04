package com.teletronics.storage.service;

import com.teletronics.storage.domain.FileStatus;
import com.teletronics.storage.domain.StoredFile;
import com.teletronics.storage.domain.Visibility;
import com.teletronics.storage.exception.DuplicateFileException;
import com.teletronics.storage.exception.OwnedFileNotFoundException;
import com.teletronics.storage.mapper.FileReservationMapper;
import com.teletronics.storage.repository.FileContentRepository;
import com.teletronics.storage.repository.FileMetadataRepository;
import com.teletronics.storage.service.command.UploadCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.teletronics.storage.exception.InvalidUploadException;
import com.teletronics.storage.exception.UploadInterruptedException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileCommandServiceTest {

    private static final String OWNER = "user-1";
    private static final String FILE_ID = "file-1";
    private static final String CONTENT_ID = "content-1";

    @Mock
    private FileMetadataRepository metadata;
    @Mock
    private FileContentRepository content;
    @Mock
    @SuppressWarnings("unused")
    private FileReservationMapper fileReservationMapper;

    @InjectMocks
    private FileCommandService service;

    @Test
    void duplicateFilenameIsRejectedBeforeContentIsStored() {
        when(metadata.reserve(any()))
                .thenThrow(new DuplicateFileException("filename already exists"));

        UploadCommand command = new UploadCommand(
                OWNER,
                "taken.txt",
                Visibility.PUBLIC,
                List.of(),
                null
        );

        ByteArrayInputStream contentStream = new ByteArrayInputStream(new byte[100]);

        assertThatThrownBy(() -> service.upload(command, contentStream))
                .isInstanceOf(DuplicateFileException.class)
                .hasMessageContaining("filename already exists");

        verifyNoInteractions(content);
    }

    @Test
    void deleteRemovesRecordThenContent() {
        when(metadata.deleteOwned(FILE_ID, OWNER)).thenReturn(create());

        service.deleteFile(OWNER, FILE_ID);

        InOrder order = inOrder(metadata, content);
        order.verify(metadata).deleteOwned(FILE_ID, OWNER);
        order.verify(content).delete(CONTENT_ID);
    }

    @Test
    void deleteOfMissingFileLeavesContentAlone() {
        when(metadata.deleteOwned(FILE_ID, OWNER)).thenThrow(new OwnedFileNotFoundException(FILE_ID, OWNER));

        assertThatThrownBy(() -> service.deleteFile(OWNER, FILE_ID))
                .isInstanceOf(OwnedFileNotFoundException.class);

        verifyNoInteractions(content);
    }

    @Test
    void renameDelegatesToRepositoryAndReturnsItsResult() {
        StoredFile renamed = create("new.txt");
        when(metadata.rename(FILE_ID, OWNER, "new.txt")).thenReturn(renamed);

        StoredFile result = service.renameFile(OWNER, FILE_ID, "new.txt");

        assertThat(result).isSameAs(renamed);
        verifyNoInteractions(content);   // a rename never touches the content
    }

    @Test
    void renameOfMissingFileFails() {
        when(metadata.rename(FILE_ID, OWNER, "new.txt"))
                .thenThrow(new OwnedFileNotFoundException(FILE_ID, OWNER));

        assertThatThrownBy(() -> service.renameFile(OWNER, FILE_ID, "new.txt"))
                .isInstanceOf(OwnedFileNotFoundException.class);
    }

    @Test
    void renameToTakenFilenameFails() {
        when(metadata.rename(FILE_ID, OWNER, "taken.txt"))
                .thenThrow(new DuplicateFileException("filename already exists"));

        assertThatThrownBy(() -> service.renameFile(OWNER, FILE_ID, "taken.txt"))
                .isInstanceOf(DuplicateFileException.class)
                .hasMessage("filename already exists");
    }

    @Test
    void interruptedWhileStoringDiscardsReservationAndNeverCompletes() throws IOException {
        when(metadata.reserve(any())).thenReturn(pending());
        when(content.store(any())).thenThrow(new IOException("connection reset"));
        UploadCommand command = command("text/plain");
        InputStream body = new ByteArrayInputStream(new byte[100]);

        assertThatThrownBy(() -> service.upload(command, body))
                .isInstanceOf(UploadInterruptedException.class)
                .hasCauseInstanceOf(IOException.class);

        verify(metadata).discard(FILE_ID);
        verify(metadata, never()).complete(any(), any(), any());
        verify(content, never()).delete(any());   // a failed store() cleans up after itself
    }

    @ParameterizedTest(name = "stream fails after {0} bytes")
    @ValueSource(ints = {0, 1})
    @SuppressWarnings("resource")
    void interruptedBeforeStoringDiscardsReservation(int deliverableBytes) {
        // 0 bytes: the emptiness check fails with IOException.
        // 1 byte: the check passes, then content type detection fails with UncheckedIOException.
        when(metadata.reserve(any())).thenReturn(pending());
        UploadCommand command = command(null);   // no type given, so detection runs
        InputStream body = failingAfter(deliverableBytes);

        assertThatThrownBy(() -> service.upload(command, body))
                .isInstanceOf(UploadInterruptedException.class);

        verify(metadata).discard(FILE_ID);
        verifyNoInteractions(content);
    }

    @Test
    void emptyContentDiscardsReservationAndRethrows() {
        when(metadata.reserve(any())).thenReturn(pending());
        UploadCommand command = command("text/plain");
        InputStream body = new ByteArrayInputStream(new byte[0]);

        assertThatThrownBy(() -> service.upload(command, body))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessage("file must not be empty");

        verify(metadata).discard(FILE_ID);
        verifyNoInteractions(content);
    }

    @Test
    void duplicateContentDiscardsReservationAndDeletesStoredContent() throws IOException {
        when(metadata.reserve(any())).thenReturn(pending());
        when(content.store(any())).thenAnswer(inv -> {
            inv.<InputStream>getArgument(0).readAllBytes();
            return CONTENT_ID;
        });
        when(metadata.complete(eq(FILE_ID), any(), anyString()))
                .thenThrow(new DuplicateFileException("same content already uploaded"));
        UploadCommand command = command("text/plain");
        InputStream body = new ByteArrayInputStream(new byte[100]);

        assertThatThrownBy(() -> service.upload(command, body))
                .isInstanceOf(DuplicateFileException.class)
                .hasMessage("same content already uploaded");

        InOrder order = inOrder(metadata, content);
        order.verify(metadata).discard(FILE_ID);
        order.verify(content).delete(CONTENT_ID);
    }

    @Test
    void missingSha256AlgorithmIsReportedAsIllegalState() {
        when(metadata.reserve(any())).thenReturn(pending());
        UploadCommand command = command("text/plain");
        InputStream body = new ByteArrayInputStream(new byte[100]);

        try (MockedStatic<MessageDigest> digest = mockStatic(MessageDigest.class)) {
            digest.when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new NoSuchAlgorithmException("no SHA-256"));

            assertThatThrownBy(() -> service.upload(command, body))
                    .isInstanceOf(IllegalStateException.class)
                    .hasCauseInstanceOf(NoSuchAlgorithmException.class);
        }
    }

    private static UploadCommand command(String contentType) {
        return new UploadCommand(OWNER, "report.txt", Visibility.PUBLIC, List.of(), contentType);
    }

    private static StoredFile pending() {
        return new StoredFile(FILE_ID, OWNER, "report.txt", Visibility.PUBLIC, Set.of(),
                null, 0, null, Instant.now(), null, null, FileStatus.PENDING);
    }

    /** Delivers the given number of bytes, then fails the way a closed client connection does. */
    private static InputStream failingAfter(int deliverableBytes) {
        return new InputStream() {
            private int delivered;

            @Override
            public int read() throws IOException {
                if (delivered >= deliverableBytes) {
                    throw new IOException("connection reset");
                }
                delivered++;
                return 'x';
            }
        };
    }

    private static StoredFile create() {
        return create("report.txt");
    }

    private static StoredFile create(String filename) {
        return new StoredFile(FILE_ID, OWNER, filename, Visibility.PUBLIC, Set.of(),
                "text/plain", 10, "sha", Instant.now(), "token", CONTENT_ID, FileStatus.READY);
    }
}