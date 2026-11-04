package com.teletronics.storage.service;

import com.teletronics.storage.domain.StoredContent;
import com.teletronics.storage.domain.StoredFile;
import com.teletronics.storage.exception.DuplicateFileException;
import com.teletronics.storage.exception.InvalidUploadException;
import com.teletronics.storage.exception.UploadInterruptedException;
import com.teletronics.storage.helper.CountingInputStream;
import com.teletronics.storage.mapper.FileReservationMapper;
import com.teletronics.storage.repository.FileContentRepository;
import com.teletronics.storage.repository.FileMetadataRepository;
import com.teletronics.storage.service.command.UploadCommand;
import com.teletronics.storage.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class FileCommandService {
    private final FileContentRepository fileContentRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileReservationMapper fileReservationMapper;

    private static final int BUFFER_SIZE = 64 * 1024;

    /**
     * Stores the uploaded content and marks the file as READY.
     * Reserves the filename with a PENDING record first, streams the content while computing
     * its size and SHA-256, then completes the record with the content reference and a download token.
     * If completing fails because the owner already has the same content, both the PENDING record
     * and the stored content are removed before the exception is rethrown.
     *
     * @param uploadCommand validated upload metadata
     * @param body          content stream; read to the end
     * @return the completed, READY file
     * @throws DuplicateFileException     if the owner already has a file with the same filename or content
     * @throws UploadInterruptedException if the content cannot be read or stored
     */
    public StoredFile upload(UploadCommand uploadCommand, InputStream body) {
        var reservation = fileReservationMapper.toReservation(uploadCommand);
        var pending = fileMetadataRepository.reserve(reservation);
        var content = storeContent(uploadCommand, body, pending);

        try {
            return fileMetadataRepository.complete(pending.id(), content, FileUtils.newDownloadToken());
        } catch (DuplicateFileException e) {
            fileMetadataRepository.discard(pending.id());
            fileContentRepository.delete(content.contentId());
            throw e;
        }
    }

    private StoredContent storeContent(UploadCommand command, InputStream body, StoredFile pending) {
        try (
                var buffered = new BufferedInputStream(body, BUFFER_SIZE);
                var counting = new CountingInputStream(buffered)
        ) {
            FileUtils.requireNotEmpty(buffered);

            var contentType = FileUtils.detectContentType(command.contentType(), buffered, command.filename());
            var sha256 = MessageDigest.getInstance("SHA-256");
            var contentId = fileContentRepository.store(new DigestInputStream(counting, sha256));

            return new StoredContent(
                    contentId,
                    HexFormat.of().formatHex(sha256.digest()),
                    counting.getCount(),
                    contentType
            );
        } catch (IOException | UncheckedIOException e) {
            fileMetadataRepository.discard(pending.id());
            throw new UploadInterruptedException(e);
        } catch (InvalidUploadException e) {
            fileMetadataRepository.discard(pending.id());
            throw e;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
