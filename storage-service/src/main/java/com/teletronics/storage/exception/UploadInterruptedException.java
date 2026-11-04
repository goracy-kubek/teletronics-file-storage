package com.teletronics.storage.exception;

/**
 * Thrown when an upload cannot be completed because reading or storing the content failed.
 */
public class UploadInterruptedException extends RuntimeException {
    /**
     * @param message description of the failure
     */
    public UploadInterruptedException(String message) {
        super(message);
    }

    /**
     * @param cause underlying failure that interrupted the upload
     */
    public UploadInterruptedException(Throwable cause) {
        super(cause);
    }
}
