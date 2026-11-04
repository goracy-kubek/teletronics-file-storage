package com.teletronics.storage.utils;

import com.teletronics.storage.exception.InvalidUploadException;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("resource")
class FileUtilsTest {

    @Test
    void emptyStreamIsRejected() {
        BufferedInputStream empty = buffered(new byte[0]);

        assertThatThrownBy(() -> FileUtils.requireNotEmpty(empty))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessage("file must not be empty");
    }

    @Test
    void nonEmptyStreamIsAccepted() {
        BufferedInputStream content = buffered(new byte[]{1, 2, 3});

        assertThatCode(() -> FileUtils.requireNotEmpty(content)).doesNotThrowAnyException();
    }

    @Test
    void singleByteStreamIsAccepted() {
        BufferedInputStream content = buffered(new byte[]{42});

        assertThatCode(() -> FileUtils.requireNotEmpty(content)).doesNotThrowAnyException();
    }

    @Test
    void byteWithValue255IsNotMistakenForEndOfStream() {
        // read() returns 0..255 for data and -1 only for the end; 0xFF must not count as empty
        BufferedInputStream content = buffered(new byte[]{(byte) 0xFF});

        assertThatCode(() -> FileUtils.requireNotEmpty(content)).doesNotThrowAnyException();
    }

    @Test
    void checkDoesNotConsumeAnyByte() throws IOException {
        byte[] data = {10, 20, 30, 40};
        BufferedInputStream content = buffered(data);

        FileUtils.requireNotEmpty(content);

        assertThat(content.readAllBytes()).isEqualTo(data);
    }

    @Test
    void readFailureIsPropagated() {
        BufferedInputStream broken = new BufferedInputStream(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("connection reset");
            }
        });

        assertThatThrownBy(() -> FileUtils.requireNotEmpty(broken))
                .isInstanceOf(IOException.class)
                .hasMessage("connection reset");
    }

    @Test
    void detectionFailureIsWrappedInUncheckedIOException() {
        IOException failure = new IOException("connection reset");
        BufferedInputStream broken = new BufferedInputStream(new InputStream() {
            @Override
            public int read() throws IOException {
                throw failure;
            }
        });

        assertThatThrownBy(() -> FileUtils.detectContentType(null, broken, "report.pdf"))
                .isInstanceOf(UncheckedIOException.class)
                .hasCause(failure);
    }

    private static BufferedInputStream buffered(byte[] data) {
        return new BufferedInputStream(new ByteArrayInputStream(data));
    }
}