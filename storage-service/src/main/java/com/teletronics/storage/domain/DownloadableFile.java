package com.teletronics.storage.domain;

import java.io.InputStream;

/**
 * A file ready to be downloaded: its metadata together with an open stream of its content.
 * The caller owns the stream and must close it after reading.
 *
 * @param file    metadata of the stored file (name, content type, size)
 * @param content stream of the file content
 */
public record DownloadableFile(StoredFile file, InputStream content) {
}