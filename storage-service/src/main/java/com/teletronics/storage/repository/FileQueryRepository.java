package com.teletronics.storage.repository;

import com.teletronics.storage.domain.StoredFile;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Read side of file metadata. Only files in status READY are ever returned:
 * uploads in progress are invisible here.
 */
public interface FileQueryRepository {

    /**
     * Finds a file by its download token. No owner or visibility check:
     * the token itself grants access to both PUBLIC and PRIVATE files.
     *
     * @return the file, or empty if the token is unknown or the file is not READY
     */
    Optional<StoredFile> findReadyByToken(String downloadToken);

    /**
     * Lists all files of the owner, both PUBLIC and PRIVATE, and never files of other users.
     *
     * @param tag      optional filter: exact match, case-insensitive, no partial matching;
     *                 an unknown tag gives an empty page, not an error
     * @param pageable page, size and sort; sortable properties are
     *                 {@code filename}, {@code uploadDate}, {@code contentType}, {@code size}, {@code tags}
     * @throws IllegalArgumentException if the sort contains another property
     */
    Page<StoredFile> findOwn(String ownerId, @Nullable String tag, Pageable pageable);

    /**
     * Lists PUBLIC files of all users, and never PRIVATE ones, whoever asks.
     * Tag filter, paging and sort work as in {@link #findOwn}.
     */
    Page<StoredFile> findPublic(@Nullable String tag, Pageable pageable);
}