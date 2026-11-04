package com.teletronics.storage.repository;

import java.io.IOException;
import java.io.InputStream;

public interface FileContentRepository {

    /**
     * Reads the stream to the end and stores its content, without buffering it whole
     * in memory or on local disk.
     * If reading or writing fails, removes any partially stored data before rethrowing.
     *
     * @return id of the stored content
     * @throws IOException if the stream cannot be read to the end
     */
    String store(InputStream content) throws IOException;

    /**
     * Deletes stored content. Does nothing if it does not exist.
     */
    void delete(String contentId);
}