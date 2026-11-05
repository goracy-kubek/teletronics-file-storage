package com.teletronics.storage.helper;

import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A stream decorator for {@link InputStream} that counts the total number
 * of bytes read from the stream in real time.
 * <p>
 * Useful for tracking transferred data sizes (e.g., during file uploads),
 * calculating progress, or logging I/O operations.
 */
@Getter
public class CountingInputStream extends FilterInputStream {
    private long count;

    public CountingInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b != -1) {
            count++;
        }
        return b;
    }

    @Override
    public int read(byte @NonNull [] buffer, int offset, int length) throws IOException {
        int n = super.read(buffer, offset, length);
        if (n > 0) {
            count += n;
        }
        return n;
    }
}
