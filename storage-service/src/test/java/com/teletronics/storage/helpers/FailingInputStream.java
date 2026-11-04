package com.teletronics.storage.helpers;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class FailingInputStream extends InputStream {

    private final long failAfter;
    private long delivered;

    public FailingInputStream(long failAfter) {
        this.failAfter = failAfter;
    }

    @Override
    public int read() throws IOException {
        failIfLimitReached();
        delivered++;
        return 'x';
    }

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