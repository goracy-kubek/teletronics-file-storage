package com.teletronics.storage.controller;

import com.teletronics.storage.api.FilesApi;
import com.teletronics.storage.api.model.FilePage;
import com.teletronics.storage.api.model.FileResponse;
import com.teletronics.storage.api.model.RenameRequest;
import com.teletronics.storage.api.model.Visibility;
import com.teletronics.storage.exception.UploadInterruptedException;
import com.teletronics.storage.mapper.FileResponseMapper;
import com.teletronics.storage.service.FileUploadService;
import com.teletronics.storage.service.command.UploadCommand;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class FileController implements FilesApi {
    private final FileUploadService fileUploadService;
    private final FileResponseMapper fileResponseMapper;

    @Override
    public ResponseEntity<Void> deleteFile(String xUserId, String id) {
        return null;
    }

    @Override
    public ResponseEntity<InputStreamResource> downloadFile(String token) {
        return null;
    }

    @Override
    public ResponseEntity<FilePage> listOwnFiles(
            String xUserId, @Nullable String tag, String sort, Integer page, Integer size) {
        return null;
    }

    @Override
    public ResponseEntity<FilePage> listPublicFiles(
            String xUserId, @Nullable String tag, String sort, Integer page, Integer size) {
        return null;
    }

    @Override
    public ResponseEntity<FileResponse> renameFile(
            String xUserId, String id, RenameRequest renameRequest) {
        return null;
    }

    @Override
    public ResponseEntity<FileResponse> uploadFile(
            String xUserId,
            String filename,
            Visibility visibility,
            InputStreamResource body,
            @Nullable List<@Size(min = 1, max = 50) String> tags,
            @Nullable String contentType
    ) {
        UploadCommand command = new UploadCommand(
                xUserId,
                filename,
                com.teletronics.storage.domain.Visibility.valueOf(visibility.name()),
                tags,
                contentType);

        try (InputStream content = body.getInputStream()) {
            var file = fileUploadService.upload(command, content);
            return ResponseEntity.status(HttpStatus.CREATED).body(fileResponseMapper.toResponse(file));
        } catch (IOException e) {
            throw new UploadInterruptedException(e);
        }
    }
}
