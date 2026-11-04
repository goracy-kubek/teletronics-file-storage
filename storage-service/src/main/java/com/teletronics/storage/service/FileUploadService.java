package com.teletronics.storage.service;

import com.teletronics.storage.domain.StoredFile;
import com.teletronics.storage.repository.FileContentRepository;
import com.teletronics.storage.repository.FileMetadataRepository;
import com.teletronics.storage.service.command.UploadCommand;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class FileUploadService {
    private final FileContentRepository fileContentRepository;
    private final FileMetadataRepository fileMetadataRepository;

    public StoredFile upload(UploadCommand uploadCommand, InputStream body) {
        throw new NotImplementedException();
    }
}
