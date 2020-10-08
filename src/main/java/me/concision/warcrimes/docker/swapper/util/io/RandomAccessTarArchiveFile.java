package me.concision.warcrimes.docker.swapper.util.io;

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

import java.io.Closeable;
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

    @Delegate(types = {InputStream.class}, excludes = {Closeable.class})
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

    public ArchiveEntryOffset tryGetEntry(@NonNull String filename) {
        ArchiveEntryOffset archiveEntry = entries.get(filename);
        if (archiveEntry == null) throw new RuntimeException("entry does not exist in archive: " + filename);
        return archiveEntry;
    }

    @Override
    public Iterator<ArchiveEntryOffset> iterator() {
        return entries.values().iterator();
    }

    // reading

    public TarArchiveEntry seek(@NonNull String filename) throws IOException {
        ArchiveEntryOffset archiveEntry = entries.get(filename);
        if (archiveEntry == null) throw new RuntimeException("entry does not exist in archive: " + filename);

        stream.seek(archiveEntry.offset);
        tarStream = new TarArchiveInputStream(stream);
        tarStream.getNextTarEntry();
        return archiveEntry.entry;
    }

    public byte[] readFullEntry(@NonNull ArchiveEntryOffset entry) throws IOException {
        return this.readFullEntry(entry.getName());
    }

    public byte[] readFullEntry(@NonNull TarArchiveEntry entry) throws IOException {
        return this.readFullEntry(entry.getName());
    }

    public byte[] readFullEntry(@NonNull String filename) throws IOException {
        TarArchiveEntry entry = this.seek(filename);
        byte[] buffer = new byte[(int) entry.getSize()];
        IOUtils.readFully(this, buffer);
        tarStream = null;
        return buffer;
    }

    // closable

    @Override
    public void close() throws IOException {
        stream.close();
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

        @Accessors(fluent = false)
        @Delegate
        TarArchiveEntry entry;
    }

    @Value
    public static class ArchiveFile {
        RandomAccessTarArchiveFile archive;
        ArchiveEntryOffset entry;
    }
}
