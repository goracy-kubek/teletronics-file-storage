package com.teletronics.storage.repository;

import com.teletronics.storage.domain.FileReservation;
import com.teletronics.storage.domain.StoredContent;
import com.teletronics.storage.domain.StoredFile;
import com.teletronics.storage.exception.DuplicateFileException;

/**
 * Metadata of stored files. File content is kept separately in {@link FileContentRepository}.
 */
public interface FileMetadataRepository {

    /**
     * Creates a record in status PENDING, reserving the filename for the owner.
     * Sets the upload date to the current time.
     * Atomic: of several concurrent calls with the same owner and filename, exactly one succeeds.
     *
     * @return the created record, with its id
     * @throws DuplicateFileException if the owner already has a file (PENDING or READY) with this filename
     */
    StoredFile reserve(FileReservation reservation);

    /**
     * Completes a PENDING record: sets the content data and the download token and switches it to READY,
     * in a single atomic update. On failure the record stays PENDING and unchanged.
     *
     * @return the completed record
     * @throws DuplicateFileException if the owner already has a file with the same SHA-256
     * @throws IllegalStateException  if there is no PENDING record with this id
     */
    StoredFile complete(String fileId, StoredContent content, String downloadToken);

    /**
     * Deletes a PENDING record, freeing its filename. Does nothing if the record does not exist.
     * Never deletes a READY record.
     */
    void discard(String fileId);
}