package com.teletronics.storage.exception;

/**
 * Thrown when an upload or rename conflicts with an existing file of the same owner:
 * the same filename or the same content (SHA-256).
 * Reported as HTTP 409.
 */
public class DuplicateFileException extends RuntimeException {
    /**
     * @param message description of the conflict
     */
    public DuplicateFileException(String message) {
        super(message);
    }
}
