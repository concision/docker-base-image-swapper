package me.concision.warcrimes.docker.swapper.util;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.experimental.Delegate;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class RandomAccessTarArchiveFile extends InputStream implements Iterable<RandomAccessTarArchiveFile.ArchiveEntryOffset> {
    @Accessors(fluent = false)
    @Getter
    private final File file;

    private final RandomAccessBufferedFileInputStream stream;
    private TarArchiveInputStream tarStream;

    @Accessors(fluent = false)
    @Getter
    private IndexState state = IndexState.UNINDEXED;
    private final Map<String, ArchiveEntryOffset> entries = new LinkedHashMap<>();

    public RandomAccessTarArchiveFile(@NonNull File file) throws IOException {
        this.file = file;
        stream = new RandomAccessBufferedFileInputStream(file);
    }

    @Delegate(types = {InputStream.class})
    @SneakyThrows(IOException.class)
    private TarArchiveInputStream tarStream() {
        this.createIndex();
        if (tarStream == null) throw new RuntimeException("a tar archive entry must be seeked");
        return tarStream;
    }

    // indexing

    public void createIndex() throws IOException {
        if (this.state == IndexState.INDEXED) return;
        if (this.state == IndexState.INVALID) throw new RuntimeException("tar archive was not able to be indexed");

        try {
            CountingInputStream countingInputStream = new CountingInputStream(stream);
            TarArchiveInputStream tarStream = new TarArchiveInputStream(countingInputStream);
            for (TarArchiveEntry entry; (entry = tarStream.getNextTarEntry()) != null; ) {
                long startingPosition = countingInputStream.getBytesRead() - TarArchiveEntry.DEFAULT_RCDSIZE;
                entries.put(entry.getName(), new ArchiveEntryOffset(startingPosition, entry));

                if (tarStream.canReadEntryData(entry)) {
                    // skips only current entry
                    IOUtils.skip(tarStream, Long.MAX_VALUE);
                }
            }
            this.state = IndexState.INDEXED;
        } catch (IOException exception) {
            this.state = IndexState.INVALID;
            throw new IOException("failed to index archive: " + file.getAbsolutePath(), exception);
        } catch (Exception exception) {
            this.state = IndexState.INVALID;
            throw new RuntimeException("failed to index archive: " + file.getAbsolutePath(), exception);
        }
    }

    // read

    public Set<String> getFilenames() throws IOException {
        this.createIndex();
        return entries.keySet();
    }

    public ArchiveEntryOffset getEntry(@NonNull String filename) {
        return entries.get(filename);
    }

    @Override
    public Iterator<ArchiveEntryOffset> iterator() {
        return entries.values().iterator();
    }

    // reading

    public TarArchiveEntry seek(@NonNull String filename) throws IOException {
        ArchiveEntryOffset entry = entries.get(filename);
        if (entry == null) throw new RuntimeException("entry does not exist in archive: " + filename);

        stream.seek(entry.offset);
        tarStream = new TarArchiveInputStream(stream);
        return tarStream.getNextTarEntry();
    }

    public byte[] readFullEntry(@NonNull String filename) throws IOException {
        TarArchiveEntry manifest = this.seek(filename);
        byte[] buffer = new byte[(int) manifest.getSize()];
        IOUtils.readFully(this, buffer);
        tarStream = null;
        return buffer;
    }

    // structures

    private enum IndexState {
        UNINDEXED,
        INVALID,
        INDEXED,
    }

    @Value
    public static class ArchiveEntryOffset {
        @Accessors(fluent = false)
        long offset;

        @Delegate
        TarArchiveEntry entry;
    }
}
