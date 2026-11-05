package com.teletronics.storage.helpers;

import org.jspecify.annotations.NonNull;

import java.io.InputStream;

/**
 * A test stream that generates a fixed number of bytes on the fly, so large content such as a multi-gigabyte
 * upload needs no memory or disk. The bytes repeat the pattern 0..255, so the value at any position is
 * {@code position % 256}; the stream ends after the given length.
 */
public class PatternInputStream extends InputStream {

    private final long length;
    private long position;

    /**
     * @param length total number of bytes the stream delivers before the end
     */
    public PatternInputStream(long length) {
        this.length = length;
    }

    /**
     * @return the next byte of the pattern, or -1 at the end of the stream
     */
    @Override
    public int read() {
        if (position >= length) {
            return -1;
        }
        return (int) (position++ & 0xFF);
    }

    /**
     * Fills the buffer with the next bytes of the pattern, as many as remain up to the length.
     *
     * @return the number of bytes delivered, 0 if {@code count} is 0, or -1 at the end of the stream
     */
    @Override
    public int read(byte @NonNull [] buffer, int offset, int count) {
        if (count == 0) {
            return 0;
        }
        if (position >= length) {
            return -1;
        }
        int n = (int) Math.min(count, length - position);
        for (int i = 0; i < n; i++) {
            buffer[offset + i] = (byte) (position + i);
        }
        position += n;
        return n;
    }
}
