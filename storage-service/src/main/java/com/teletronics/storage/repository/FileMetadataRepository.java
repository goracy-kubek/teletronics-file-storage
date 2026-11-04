package com.teletronics.storage.repository;

import com.teletronics.storage.domain.FileReservation;
import com.teletronics.storage.domain.StoredContent;
import com.teletronics.storage.domain.StoredFile;
import com.teletronics.storage.exception.DuplicateFileException;
import com.teletronics.storage.exception.OwnedFileNotFoundException;

/**
 * Metadata of stored files. File content is kept separately in {@link FileContentRepository}.
 * <p>
 * A file starts as a PENDING record created by {@link #reserve(FileReservation)} before its content
 * is known, and becomes READY through {@link #complete(String, StoredContent, String)}.
 * Implementations enforce filename uniqueness per owner and content (SHA-256) uniqueness per owner.
 */
public interface FileMetadataRepository {

    /**
     * Creates a record in status PENDING, reserving the filename for the owner.
     * Sets the upload date to the current time; content-related fields are not set until completion.
     * Atomic: of several concurrent calls with the same owner and filename, exactly one succeeds.
     *
     * @param reservation metadata used to create the pending record
     * @return the created record, with its id
     * @throws DuplicateFileException if the owner already has a file (PENDING or READY) with this filename
     */
    StoredFile reserve(FileReservation reservation);

    /**
     * Completes a PENDING record: sets the content data and the download token and switches it to READY,
     * in a single atomic update. On failure the record stays PENDING and unchanged.
     *
     * @param fileId        id of the PENDING record to complete
     * @param content       content data collected while storing the uploaded bytes
     * @param downloadToken token of the download link, unique across all files
     * @return the completed record
     * @throws DuplicateFileException if the owner already has a file with the same SHA-256,
     *                                or the download token is already in use
     * @throws IllegalStateException  if there is no PENDING record with this id
     */
    StoredFile complete(String fileId, StoredContent content, String downloadToken);

    /**
     * Deletes a PENDING record, freeing its filename. Does nothing if the record does not exist.
     * Never deletes a READY record.
     *
     * @param fileId id of the PENDING record to delete
     */
    void discard(String fileId);

    /**
     * Deletes the record of a READY file of this owner.
     *
     * @return the deleted file, so the caller can delete its content
     * @throws OwnedFileNotFoundException if the file is not found among the READY files of this owner
     *                                    (missing, pending or owned by another user)
     */
    StoredFile deleteOwned(String fileId, String ownerId);

    /**
     * Changes the filename of a READY file of this owner, in one atomic operation.
     *
     * @return the updated file
     * @throws OwnedFileNotFoundException if the file is not found among the READY files of this owner
     * @throws DuplicateFileException     if the owner already has a file with this filename
     */
    StoredFile rename(String fileId, String ownerId, String newFilename);
}