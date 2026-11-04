package com.teletronics.storage.domain;

/**
 * Upload state: PENDING while content is being uploaded, READY when the file is available.
 */
public enum FileStatus {
    PENDING,
    READY
}