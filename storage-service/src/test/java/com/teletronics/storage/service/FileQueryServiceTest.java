package com.teletronics.storage.service;

import com.teletronics.storage.domain.StoredFile;
import com.teletronics.storage.repository.FileContentRepository;
import com.teletronics.storage.repository.FileQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import com.teletronics.storage.domain.DownloadableFile;
import com.teletronics.storage.domain.FileStatus;
import com.teletronics.storage.domain.Visibility;
import com.teletronics.storage.exception.ContentNotFoundException;
import com.teletronics.storage.exception.DownloadLinkNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("File query service")
class FileQueryServiceTest {

    private static final String TOKEN = "token-1";
    private static final String CONTENT_ID = "content-1";

    @Mock
    private FileQueryRepository fileQueryRepository;
    @Mock
    private FileContentRepository fileContentRepository;

    @InjectMocks
    private FileQueryService service;

    @Test
    @DisplayName("Public list is delegated to the repository with the tag and the pageable")
    void listPublicFilesDelegatesWithTagAndPageable() {
        Pageable pageable = PageRequest.of(2, 5, Sort.by("size"));
        Page<StoredFile> expected = new PageImpl<>(List.of(), pageable, 0);
        when(fileQueryRepository.findPublic("work", pageable)).thenReturn(expected);

        Page<StoredFile> result = service.listPublicFiles("work", pageable);

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("Public list without a tag passes null")
    void listPublicFilesWithoutTagPassesNull() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<StoredFile> expected = new PageImpl<>(List.of(), pageable, 0);
        when(fileQueryRepository.findPublic(null, pageable)).thenReturn(expected);

        Page<StoredFile> result = service.listPublicFiles(null, pageable);

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("Own list is delegated to the repository with the owner, the tag and the pageable")
    void listOwnFilesDelegatesWithOwnerTagAndPageable() {
        Pageable pageable = PageRequest.of(1, 7, Sort.by("size").descending());
        Page<StoredFile> expected = new PageImpl<>(List.of(), pageable, 0);
        when(fileQueryRepository.findOwn("user-1", "work", pageable)).thenReturn(expected);

        Page<StoredFile> result = service.listOwnFiles("user-1", "work", pageable);

        assertThat(result).isSameAs(expected);
        assertThat(result.getSize()).isEqualTo(7);   // the requested page size is kept
        assertThat(result.getNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("Own list without a tag passes null")
    void listOwnFilesWithoutTagPassesNull() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<StoredFile> expected = new PageImpl<>(List.of(), pageable, 0);
        when(fileQueryRepository.findOwn("user-1", null, pageable)).thenReturn(expected);

        Page<StoredFile> result = service.listOwnFiles("user-1", null, pageable);

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("Own list asks only for the given owner and never for the public list")
    void listOwnFilesAsksOnlyForTheGivenOwner() {
        Pageable pageable = PageRequest.of(0, 20);
        when(fileQueryRepository.findOwn(anyString(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.listOwnFiles("user-1", null, pageable);

        verify(fileQueryRepository).findOwn("user-1", null, pageable);
        verify(fileQueryRepository, never()).findPublic(any(), any());
    }

    @Test
    @DisplayName("Download returns the file and its content stream")
    void downloadReturnsFileAndItsContentStream() {
        StoredFile file = create();
        InputStream stream = new ByteArrayInputStream("hello".getBytes(UTF_8));
        when(fileQueryRepository.findReadyByToken(TOKEN)).thenReturn(Optional.of(file));
        when(fileContentRepository.download(CONTENT_ID)).thenReturn(stream);

        DownloadableFile result = service.download(TOKEN);

        assertThat(result.file()).isSameAs(file);
        assertThat(result.content()).isSameAs(stream);
    }

    @Test
    @DisplayName("Download with an unknown token fails without opening any content")
    void downloadOfUnknownTokenFailsWithoutOpeningContent() {
        when(fileQueryRepository.findReadyByToken(TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(TOKEN))
                .isInstanceOf(DownloadLinkNotFoundException.class);

        verifyNoInteractions(fileContentRepository);
    }

    @Test
    @DisplayName("Download opens the content of the found file")
    @SuppressWarnings("resource")
    void downloadOpensContentOfTheFoundFile() {
        when(fileQueryRepository.findReadyByToken(TOKEN)).thenReturn(Optional.of(create()));
        when(fileContentRepository.download(CONTENT_ID)).thenReturn(InputStream.nullInputStream());

        service.download(TOKEN);

        verify(fileContentRepository).download(CONTENT_ID);
    }

    @Test
    @DisplayName("Download of a record without content is reported as missing content, not as a missing link")
    void downloadOfRecordWithoutContentIsNotReportedAsMissingLink() {
        when(fileQueryRepository.findReadyByToken(TOKEN)).thenReturn(Optional.of(create()));
        when(fileContentRepository.download(CONTENT_ID)).thenThrow(new ContentNotFoundException(CONTENT_ID));

        assertThatThrownBy(() -> service.download(TOKEN))
                .isInstanceOf(ContentNotFoundException.class);
    }

    @Test
    @DisplayName("Download error message does not reveal the token")
    void downloadErrorMessageDoesNotRevealTheToken() {
        when(fileQueryRepository.findReadyByToken(TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(TOKEN))
                .hasMessageNotContaining(TOKEN);
    }

    private static StoredFile create() {
        return new StoredFile("file-1", "user-1", "report.txt", Visibility.PRIVATE, Set.of(),
                "text/plain", 5, "sha", Instant.now(), TOKEN, CONTENT_ID, FileStatus.READY);
    }
}