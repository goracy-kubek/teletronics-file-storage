package com.teletronics.storage.exception;

/**
 * Thrown when the content of a file cannot be found in the content repository: it was never stored,
 * was deleted, or the id has an unknown format.
 * Since a metadata record always points to stored content, this signals an inconsistency
 * and is reported as an internal error (HTTP 500).
 */
public class ContentNotFoundException extends RuntimeException {
    /**
     * @param contentId id of the content that was not found
     */
    public ContentNotFoundException(String contentId) {
        super("Content not found: " + contentId);
    }
}
