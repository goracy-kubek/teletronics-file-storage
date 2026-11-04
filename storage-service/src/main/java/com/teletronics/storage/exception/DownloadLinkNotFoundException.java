package com.teletronics.storage.exception;

public class DownloadLinkNotFoundException extends RuntimeException {
    public DownloadLinkNotFoundException(String token) {
        super("Download link not found: " + token);
    }
}
