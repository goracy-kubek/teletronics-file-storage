package com.teletronics.storage.domain;

import java.io.InputStream;

public record DownloadableFile(StoredFile file, InputStream content) {
}