package com.teletronics.storage.exception;

public class OwnedFileNotFoundException extends RuntimeException {
    public OwnedFileNotFoundException(String fileId, String ownerId) {
        super("File " + fileId + " not found among files of user " + ownerId);
    }
}