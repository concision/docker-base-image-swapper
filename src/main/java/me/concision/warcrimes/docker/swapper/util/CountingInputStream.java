package me.concision.warcrimes.docker.swapper.util;

import lombok.NonNull;
import org.apache.commons.compress.utils.IOUtils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Concision
 */
class CountingInputStream extends FilterInputStream {
    // write-only buffer
    private static final byte[] SKIP_BUFFER = new byte[4096];

    private long bytesRead;

    public CountingInputStream(@NonNull InputStream inputStream) {
        super(inputStream);
    }

    public long getBytesRead() {
        return bytesRead;
    }

    protected void count(long read) {
        if (0 < read) {
            bytesRead += read;
        }
    }

    // read

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (0 <= b) {
            count(1);
        }
        return b;
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (length == 0) {
            return 0;
        }
        int read = in.read(buffer, offset, length);
        this.count(read);
        return read;
    }

    // skip

    @Override
    public long skip(long n) throws IOException {
        long available = n;

        while (0 < n) {
            long skipped = in.skip(n);
            if (skipped == 0) {
                break;
            }
            this.count(skipped);
            n -= skipped;
        }

        while (0 < n) {
            int read = IOUtils.readFully(this, SKIP_BUFFER, 0, (int) Math.min(n, SKIP_BUFFER.length));
            if (read <= 0) {
                break;
            }
            n -= read;
        }

        return available - n;
    }
}