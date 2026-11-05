package com.teletronics.storage.utils;

import com.teletronics.storage.helper.CountingInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Counting input stream")
class CountingInputStreamTest {

    @Test
    @DisplayName("A single-byte read is counted")
    void singleByteReadIsCounted() throws IOException {
        CountingInputStream in = new CountingInputStream(new ByteArrayInputStream(new byte[]{10, 20, 30}));

        int first = in.read();

        assertThat(first).isEqualTo(10);
        assertThat(in.getCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Every single-byte read is counted")
    @SuppressWarnings("unused")
    void everySingleByteReadIsCounted() throws IOException {
        CountingInputStream in = new CountingInputStream(new ByteArrayInputStream(new byte[]{10, 20, 30}));

        assertThat(in.read()).isEqualTo(10);
        assertThat(in.read()).isEqualTo(20);
        assertThat(in.read()).isEqualTo(30);

        assertThat(in.getCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("End of stream is not counted")
    void endOfStreamIsNotCounted() throws IOException {
        CountingInputStream in = new CountingInputStream(new ByteArrayInputStream(new byte[0]));

        int result = in.read();

        assertThat(result).isEqualTo(-1);
        assertThat(in.getCount()).isZero();
    }

    @Test
    @DisplayName("Reading past the end keeps the count")
    void readingPastTheEndKeepsTheCount() throws IOException {
        CountingInputStream in = new CountingInputStream(new ByteArrayInputStream(new byte[]{1, 2}));
        in.readNBytes(2);

        int pastEnd = in.read();

        assertThat(pastEnd).isEqualTo(-1);
        assertThat(in.getCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("A byte with value 255 is not mistaken for end of stream")
    void byteWithValue255IsNotMistakenForEndOfStream() throws IOException {
        // read() returns 0..255 for data and -1 only at the end, so 0xFF must be counted
        CountingInputStream in = new CountingInputStream(new ByteArrayInputStream(new byte[]{(byte) 0xFF}));

        int result = in.read();

        assertThat(result).isEqualTo(255);
        assertThat(in.getCount()).isEqualTo(1);
    }
}