package com.teletronics.storage.domain;

import com.teletronics.storage.api.model.Visibility;

import java.time.Instant;
import java.util.Set;

/**
 * A stored file: its metadata and a reference to its content.
 * Independent of both the database and the HTTP API.
 *
 * @param id            id of the file record
 * @param ownerId       id of the user who uploaded the file
 * @param filename      filename, unique per owner
 * @param visibility    whether the file appears in the public list
 * @param tags          normalized tags, at most 5
 * @param contentType   MIME type, provided or detected; {@code null} while PENDING
 * @param size          content size in bytes; 0 while PENDING
 * @param sha256        hex-encoded SHA-256 of the content, unique per owner; {@code null} while PENDING
 * @param uploadDate    time when the upload was started
 * @param downloadToken random token of the download link; {@code null} while PENDING
 * @param contentId     id of the content in the content repository; {@code null} while PENDING
 * @param status        PENDING while uploading, READY when available
 */
public record StoredFile(
        String id,
        String ownerId,
        String filename,
        Visibility visibility,
        Set<String> tags,
        String contentType,
        long size,
        String sha256,
        Instant uploadDate,
        String downloadToken,
        String contentId,
        FileStatus status
) {
    public StoredFile {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }
}