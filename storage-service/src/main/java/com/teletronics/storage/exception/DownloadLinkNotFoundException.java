package com.teletronics.storage.exception;

public class DownloadLinkNotFoundException extends RuntimeException {
    public DownloadLinkNotFoundException() {
        super("Download link not found");
    }
}
