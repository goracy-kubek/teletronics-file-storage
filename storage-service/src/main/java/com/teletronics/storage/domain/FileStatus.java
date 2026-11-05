package com.teletronics.storage.domain;

/**
 * Upload state of a file: {@link #PENDING} while content is being uploaded,
 * {@link #READY} when the file is available.
 * Only READY files can be listed, downloaded, renamed or deleted.
 */
public enum FileStatus {
    /**
     * The filename is reserved, but the content is not stored yet.
     * Content data and the download token are absent. Removed if the upload fails.
     */
    PENDING,
    /** The content is stored and the file is available. */
    READY
}