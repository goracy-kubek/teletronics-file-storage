package com.teletronics.storage.exception;

/**
 * Thrown when an upload conflicts with an existing file
 */
public class DuplicateFileException extends RuntimeException {
    /**
     * @param message description of the conflict
     */
    public DuplicateFileException(String message) {
        super(message);
    }
}
