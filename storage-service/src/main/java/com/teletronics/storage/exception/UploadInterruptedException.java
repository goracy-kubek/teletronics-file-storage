package com.teletronics.storage.exception;

/**
 * Thrown when an upload cannot be completed because reading or storing the content failed,
 * for example when the client disconnects midway.
 * Reported as HTTP 400.
 */
public class UploadInterruptedException extends RuntimeException {

    /**
     * @param cause underlying failure that interrupted the upload
     */
    public UploadInterruptedException(Throwable cause) {
        super(cause);
    }
}
