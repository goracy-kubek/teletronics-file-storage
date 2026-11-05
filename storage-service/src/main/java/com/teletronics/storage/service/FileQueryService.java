package com.teletronics.storage.service;

import com.teletronics.storage.domain.DownloadableFile;
import com.teletronics.storage.domain.StoredFile;
import com.teletronics.storage.exception.DownloadLinkNotFoundException;
import com.teletronics.storage.repository.FileContentRepository;
import com.teletronics.storage.repository.FileQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileQueryService {
    private final FileQueryRepository fileQueryRepository;
    private final FileContentRepository fileContentRepository;

    /**
     * Finds a file by its download link and opens its content.
     * No owner or visibility check: the token itself grants access to both PUBLIC and PRIVATE files.
     *
     * @param token token from the download link
     * @return the file and a stream of its content; the caller must close the stream
     * @throws DownloadLinkNotFoundException if the token is unknown or the file is not READY
     */
    public DownloadableFile download(String token) {
        return fileQueryRepository.findReadyByToken(token)
                .map(file -> {
                    log.debug("Download: fileId={}, ownerId={}", file.id(), file.ownerId());
                    return new DownloadableFile(file, fileContentRepository.download(file.contentId()));
                })
                .orElseThrow(DownloadLinkNotFoundException::new);
    }

    /**
     * Lists all READY files of the owner, both PUBLIC and PRIVATE.
     *
     * @param ownerId  id of the user who owns the files
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