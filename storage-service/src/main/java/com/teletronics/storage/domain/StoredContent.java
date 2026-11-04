package com.teletronics.storage.domain;

/**
 * Data about stored content, known only after the whole stream has been read.
 */
public record StoredContent(
        String contentId,
        String sha256,
        long size,
        String contentType
) {
}
