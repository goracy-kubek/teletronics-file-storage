package com.teletronics.storage.helpers;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * A test stream that delivers a fixed number of bytes and then fails with an {@link IOException}.
 * Simulates a client that disconnects in the middle of an upload.
 * Every delivered byte is {@code 'x'}; the stream never reaches the end on its own.
 */
public class FailingInputStream extends InputStream {

    private final long failAfter;
    private long delivered;

    /**
     * @param failAfter number of bytes to deliver before the next read fails
     */
    public FailingInputStream(long failAfter) {
        this.failAfter = failAfter;
    }

    /**
     * @return the byte {@code 'x'}
     * @throws IOException once {@code failAfter} bytes have been delivered
     */
    @Override
    public int read() throws IOException {
        failIfLimitReached();
        delivered++;
        return 'x';
    }

    /**
     * Fills the buffer with {@code 'x'}, delivering at most the bytes left before the limit,
     * so a read that crosses the limit returns the remaining bytes and the next read fails.
     *
     * @return the number of bytes delivered, or 0 if {@code length} is 0
     * @throws IOException once {@code failAfter} bytes have been delivered
     */
    @Override
    public int read(byte @NonNull [] buffer, int offset, int length) throws IOException {
        if (length == 0) {
            return 0;
        }
        failIfLimitReached();
        int count = (int) Math.min(length, failAfter - delivered);
        Arrays.fill(buffer, offset, offset + count, (byte) 'x');
        delivered += count;
        return count;
    }

    private void failIfLimitReached() throws IOException {
        if (delivered >= failAfter) {
            throw new IOException("Simulated client disconnect after " + delivered + " bytes");
        }
    }
}