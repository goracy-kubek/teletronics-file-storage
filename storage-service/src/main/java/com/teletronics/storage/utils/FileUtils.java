package com.teletronics.storage.utils;

import com.teletronics.storage.exception.InvalidUploadException;
import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;
import org.apache.tika.Tika;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@UtilityClass
public final class FileUtils {
    private static final Tika TIKA = new Tika();
    private static final int DETECTION_BYTES = 8 * 1024;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Returns the provided content type, or detects it from the first bytes of the content
     * and the filename. The stream is left at its starting position, so no bytes are lost.
     *
     * @param content stream supporting mark/reset, with a buffer of at least 8 KB
     * @return a MIME type; {@code application/octet-stream} if it cannot be detected
     */
    public static String detectContentType(@Nullable String contentType, BufferedInputStream content, String filename) {
        return Optional.ofNullable(contentType)
                .orElseGet(() -> detect(content, filename));
    }

    public static void requireNotEmpty(BufferedInputStream content) throws IOException {
        content.mark(1);
        boolean empty = content.read() == -1;
        content.reset();
        if (empty) {
            throw new InvalidUploadException("file must not be empty");
        }
    }

    public static String newDownloadToken() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String detect(BufferedInputStream content, String filename) {
        try {
            content.mark(DETECTION_BYTES);
            byte[] head = content.readNBytes(DETECTION_BYTES);
            content.reset();
            return TIKA.detect(head, filename);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}