package com.teletronics.storage.service;

import com.teletronics.storage.domain.Visibility;
import com.teletronics.storage.exception.DuplicateFileException;
import com.teletronics.storage.mapper.FileReservationMapper;
import com.teletronics.storage.repository.FileContentRepository;
import com.teletronics.storage.repository.FileMetadataRepository;
import com.teletronics.storage.service.command.UploadCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileMetadataRepository metadata;
    @Mock
    private FileContentRepository content;
    @Mock
    private FileReservationMapper fileReservationMapper;

    @InjectMocks
    private FileUploadService service;

    @Test
    void duplicateFilenameIsRejectedBeforeContentIsStored() {
        when(metadata.reserve(any()))
                .thenThrow(new DuplicateFileException("filename already exists"));

        UploadCommand command = new UploadCommand(
                "user-1",
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
}