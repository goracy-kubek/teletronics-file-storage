package com.teletronics.storage.controller;

import com.teletronics.storage.api.FilesApi;
import com.teletronics.storage.api.model.FilePage;
import com.teletronics.storage.api.model.FileResponse;
import com.teletronics.storage.api.model.RenameRequest;
import com.teletronics.storage.api.model.Visibility;
import com.teletronics.storage.domain.DownloadableFile;
import com.teletronics.storage.domain.StoredFile;
import com.teletronics.storage.exception.UploadInterruptedException;
import com.teletronics.storage.mapper.FileResponseMapper;
import com.teletronics.storage.service.FileCommandService;
import com.teletronics.storage.service.FileQueryService;
import com.teletronics.storage.domain.UploadCommand;
import com.teletronics.storage.utils.PageUtils;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class FileController implements FilesApi {
    private final FileCommandService fileCommandService;
    private final FileQueryService fileQueryService;
    private final FileResponseMapper fileResponseMapper;

    @Override
    public ResponseEntity<InputStreamResource> downloadFile(String token) {
        DownloadableFile download = fileQueryService.download(token);
        StoredFile file = download.file();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(new InputStreamResource(download.content()));
    }

    @Override
    public ResponseEntity<FilePage> listOwnFiles(
            String xUserId,
            @Nullable String tag,
            String sort,
            Integer page,
            Integer size
    ) {
        Page<StoredFile> files = fileQueryService.listOwnFiles(xUserId, tag, PageUtils.pageableOf(sort, page, size));
        return ResponseEntity.ok(fileResponseMapper.toPage(files));
    }

    @Override
    public ResponseEntity<FilePage> listPublicFiles(
            @Nullable String tag,
            String sort,
            Integer page,
            Integer size
    ) {
        Page<StoredFile> files = fileQueryService.listPublicFiles(tag, PageUtils.pageableOf(sort, page, size));
        return ResponseEntity.ok(fileResponseMapper.toPage(files));
    }

    @Override
    public ResponseEntity<Void> deleteFile(String xUserId, String fileId) {
        fileCommandService.delete(xUserId, fileId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<FileResponse> renameFile(
            String xUserId,
            String fileId,
            RenameRequest renameRequest
    ) {
        StoredFile renamed = fileCommandService.rename(xUserId, fileId, renameRequest.getFilename());
        return ResponseEntity.ok(fileResponseMapper.toResponse(renamed));
    }

    @Override
    public ResponseEntity<FileResponse> uploadFile(
            String xUserId,
            String filename,
            Visibility visibility,
            InputStreamResource body,
            @Nullable List<String> tags,
            @Nullable String contentType
    ) {
        UploadCommand command = new UploadCommand(
                xUserId,
                filename,
                com.teletronics.storage.domain.Visibility.valueOf(visibility.name()),
                tags,
                contentType);

        try (InputStream content = body.getInputStream()) {
            var file = fileCommandService.upload(command, content);
            return ResponseEntity.status(HttpStatus.CREATED).body(fileResponseMapper.toResponse(file));
        } catch (IOException e) {
            throw new UploadInterruptedException(e);
        }
    }
}
