package com.teletronics.storage.exception;

/**
 * Thrown when a download token is unknown or its file is not READY.
 * Reported as HTTP 404.
 */
public class DownloadLinkNotFoundException extends RuntimeException {
    public DownloadLinkNotFoundException() {
        super("Download link not found");
    }
}
