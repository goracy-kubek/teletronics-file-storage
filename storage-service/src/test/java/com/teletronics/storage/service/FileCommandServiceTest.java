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

    private static StoredFile create() {
        return new StoredFile(FILE_ID, OWNER, "report.txt", Visibility.PUBLIC, Set.of(),
                "text/plain", 10, "sha", Instant.now(), "token", CONTENT_ID, FileStatus.READY);
    }
}