package com.teletronics.storage.exception;

/**
 * Thrown when an upload request is invalid, for example when the content is empty.
 * Reported as HTTP 400.
 */
public class InvalidUploadException extends RuntimeException {
    /**
     * @param message description of the validation failure
     */
    public InvalidUploadException(String message) {
        super(message);
    }
}
