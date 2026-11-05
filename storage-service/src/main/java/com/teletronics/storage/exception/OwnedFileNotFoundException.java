package com.teletronics.storage.exception;

/**
 * Thrown when a file does not exist among the READY files of the given owner.
 * A file of another user is treated the same as a missing one, so its existence is not revealed.
 * Reported as HTTP 404.
 */
public class OwnedFileNotFoundException extends RuntimeException {
    /**
     * @param fileId  id of the requested file
     * @param ownerId id of the user who made the request
     */
    public OwnedFileNotFoundException(String fileId, String ownerId) {
        super("File " + fileId + " not found among files of user " + ownerId);
    }
}
