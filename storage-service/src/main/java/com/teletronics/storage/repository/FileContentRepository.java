package com.teletronics.storage.repository;

import com.teletronics.storage.exception.ContentNotFoundException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Stored file content, kept separately from metadata (see {@link FileMetadataRepository}).
 */
public interface FileContentRepository {

    /**
     * Reads the stream to the end and stores its content, without buffering it whole
     * in memory or on local disk. The supplied stream is not closed.
     * If reading the stream fails, any partially stored data is removed before rethrowing.
     *
     * @param content stream to read to the end
     * @return id of the stored content
     * @throws IOException if the stream cannot be read to the end
     */
    String store(InputStream content) throws IOException;

    /**
     * Deletes stored content. Does nothing if it does not exist.
     *
     * @param contentId id of the content to delete
     */
    void delete(String contentId);

    /**
     * Opens stored content for streaming, from its first byte.
     * The content is read lazily, never loaded whole into memory.
     * Every call returns a new, independent stream; the caller must close it.
     *
     * @param contentId id of the content to open
     * @throws ContentNotFoundException if there is no content with this id: it was never stored,
     *                                  was deleted, or the id has an unknown format
     */
    InputStream download(String contentId);
}