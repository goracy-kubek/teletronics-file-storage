package com.teletronics.storage.domain;

/**
 * Data about stored content, known only after the whole stream has been read.
 * Used to complete a {@link FileStatus#PENDING} record and switch it to {@link FileStatus#READY}.
 *
 * @param contentId   id of the content in the content repository
 * @param sha256      hex-encoded SHA-256 of the content
 * @param size        content size in bytes
 * @param contentType MIME type, provided or detected
 */
public record StoredContent(
        String contentId,
        String sha256,
        long size,
        String contentType
) {
}
