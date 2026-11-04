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

/**
 * Stateless helpers for handling uploaded file content
 */
@UtilityClass
public final class FileUtils {
    private static final Tika TIKA = new Tika();
    private static final int DETECTION_BYTES = 8 * 1024;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Returns the provided content type, or detects it from the first bytes of the content
     * and the filename. The stream is left at its starting position, so no bytes are lost.
     *
     * @param contentType content type supplied by the client, or {@code null} to detect it
     * @param content     stream supporting mark/reset, with a buffer of at least 8 KB
     * @param filename    original filename used as a detection hint
     * @return a MIME type; {@code application/octet-stream} if it cannot be detected
     * @throws UncheckedIOException if reading from the stream fails
     */
    public static String detectContentType(@Nullable String contentType, BufferedInputStream content, String filename) {
        return Optional.ofNullable(contentType)
                .orElseGet(() -> detect(content, filename));
    }

    /**
     * Verifies that the stream contains at least one byte without consuming any content.
     * The stream is left at its starting position.
     *
     * @param content stream supporting mark/reset
     * @throws InvalidUploadException if the stream is empty
     * @throws IOException            if marking, reading, or resetting the stream fails
     */
    public static void requireNotEmpty(BufferedInputStream content) throws IOException {
        content.mark(1);
        boolean empty = content.read() == -1;
        content.reset();
        if (empty) {
            throw new InvalidUploadException("file must not be empty");
        }
    }

    /**
     * Generates a new URL-safe download token from 128 random bits.
     *
     * @return a Base64 URL-encoded token without padding
     */
    public static String newDownloadToken() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Detects the content type from the first bytes of the stream and the filename.
     * The stream is left at its starting position.
     *
     * @param content  stream supporting mark/reset
     * @param filename original filename used as a detection hint
     * @return the detected MIME type
     * @throws UncheckedIOException if reading from or resetting the stream fails
     */
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