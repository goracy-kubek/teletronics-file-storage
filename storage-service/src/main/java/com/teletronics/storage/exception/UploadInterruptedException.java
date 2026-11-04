package com.teletronics.storage.exception;

public class UploadInterruptedException extends RuntimeException {
    public UploadInterruptedException(String message) {
        super(message);
    }

    public UploadInterruptedException(Throwable cause) {
        super(cause);
    }
}
