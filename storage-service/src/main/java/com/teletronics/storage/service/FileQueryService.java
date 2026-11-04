package com.teletronics.storage.service;

import com.teletronics.storage.domain.StoredFile;
import com.teletronics.storage.repository.FileQueryRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileQueryService {

    private final FileQueryRepository fileQueryRepository;

    /**
     * Lists all READY files of the owner, both PUBLIC and PRIVATE.
     *
     * @param tag      optional exact tag filter, case-insensitive
     * @param pageable page, size and sort
     */
    public Page<StoredFile> listOwnFiles(String ownerId, @Nullable String tag, Pageable pageable) {
        return fileQueryRepository.findOwn(ownerId, tag, pageable);
    }

    /**
     * Lists PUBLIC READY files of all users.
     *
     * @param tag      optional exact tag filter, case-insensitive
     * @param pageable page, size and sort
     */
    public Page<StoredFile> listPublicFiles(@Nullable String tag, Pageable pageable) {
        return fileQueryRepository.findPublic(tag, pageable);
    }
}