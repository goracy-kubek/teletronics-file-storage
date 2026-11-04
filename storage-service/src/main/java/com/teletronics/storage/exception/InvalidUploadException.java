package com.teletronics.storage.exception;

/**
 * Thrown when an upload request is invalid
 */
public class InvalidUploadException extends RuntimeException {
    /**
     * @param message description of the validation failure
     */
    public InvalidUploadException(String message) {
        super(message);
    }
}
