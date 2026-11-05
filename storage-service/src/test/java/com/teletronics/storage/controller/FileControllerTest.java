package com.teletronics.storage.controller;

import com.teletronics.storage.api.model.Visibility;
import com.teletronics.storage.exception.UploadInterruptedException;
import com.teletronics.storage.mapper.FileResponseMapper;
import com.teletronics.storage.service.FileCommandService;
import com.teletronics.storage.service.FileQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("File controller")
class FileControllerTest {

    @Mock
    private FileCommandService fileCommandService;
    @Mock
    @SuppressWarnings("unused")
    private FileQueryService fileQueryService;
    @Mock
    @SuppressWarnings("unused")
    private FileResponseMapper fileResponseMapper;

    @InjectMocks
    private FileController controller;

    @Test
    @DisplayName("Upload whose request body cannot be opened is reported as interrupted, without storing anything")
    void uploadOfUnopenableBodyIsReportedAsInterrupted() throws IOException {
        IOException failure = new IOException("connection reset");
        InputStreamResource body = mock(InputStreamResource.class);
        when(body.getInputStream()).thenThrow(failure);

        assertThatThrownBy(() -> controller.uploadFile(
                "user-1", "report.txt", Visibility.PRIVATE, body, null, null))
                .isInstanceOf(UploadInterruptedException.class)
                .hasCause(failure);

        verifyNoInteractions(fileCommandService);
    }
}
