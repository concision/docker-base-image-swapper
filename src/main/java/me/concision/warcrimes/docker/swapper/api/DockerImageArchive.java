package me.concision.warcrimes.docker.swapper.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.Data;
import lombok.NonNull;
import me.concision.warcrimes.docker.swapper.util.io.RandomAccessTarArchiveFile;
import me.concision.warcrimes.docker.swapper.util.io.RandomAccessTarArchiveFile.ArchiveEntryOffset;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
public class DockerImageArchive {
    protected TarArchiveEntry repositoryEntry;
    protected JsonObject repositoryJson;

    protected TarArchiveEntry manifestEntry;
    protected List<DockerImage> images = new ArrayList<>();


    public void addImage(@NonNull DockerImage image) {
        images.add(image);
    }


    public static DockerImageArchive from(@NonNull RandomAccessTarArchiveFile tarArchive) throws IOException {
        DockerImageArchive imageArchive = new DockerImageArchive();

        // parse repositories
        ArchiveEntryOffset repositoriesOffset = tarArchive.getEntry("repositories");
        if (repositoriesOffset != null) {
            imageArchive.repositoryEntry(repositoriesOffset.getEntry());
            // parse manifest
            String repositoryString = new String(tarArchive.readFullEntry(repositoriesOffset));
            JsonObject repositoryJson;
            try {
                repositoryJson = new Gson().fromJson(repositoryString, JsonObject.class);
            } catch (JsonSyntaxException exception) {
                throw new JsonSyntaxException("failed to parse 'repositories' file as a JSON object", exception);
            }
            imageArchive.repositoryJson(repositoryJson);
        }

        try {
            // read manifest
            TarArchiveEntry manifestEntry = tarArchive.tryGetEntry("manifest.json").getEntry();
            imageArchive.manifestEntry(manifestEntry);

            // parse manifest
            String manifestString = new String(tarArchive.readFullEntry(manifestEntry));
            JsonArray manifestJsonArray;
            try {
                manifestJsonArray = new Gson().fromJson(manifestString, JsonArray.class);
            } catch (JsonSyntaxException exception) {
                throw new JsonSyntaxException("failed to parse file as a JSON array", exception);
            }

            // read each image in manifest
            for (int i = 0; i < manifestJsonArray.size(); i++) {
                // read manifest element
                JsonElement manifestElementJsonElement = manifestJsonArray.get(i);

                // validate manifest element
                if (!manifestElementJsonElement.isJsonObject()) {
                    throw new RuntimeException("unexpected non-object element in array: [" + i + "]");
                }

                // read manifest element
                JsonObject manifestElementJsonObject = manifestElementJsonElement.getAsJsonObject();
                try {
                    imageArchive.addImage(DockerImage.from(tarArchive, imageArchive, manifestElementJsonObject));
                } catch (RuntimeException exception) {
                    throw new RuntimeException("failed to read image manifest from: [" + i + "]", exception);
                }
            }
        } catch (RuntimeException exception) {
            throw new RuntimeException("failed to parse 'manifest.json' file", exception);
        }

        return imageArchive;
    }

    // writing

//    private interface InputSource {
//        InputStream stream(TarArchiveEntry entry) throws IOException;
//    }
//
//    @RequiredArgsConstructor
//    private static class ByteSource implements InputSource {
//        byte[] bytes;
//
//
//
//        @Override
//        public InputStream stream() {
//            return new ByteArrayInputStream(bytes);
//        }
//    }
//
//    @RequiredArgsConstructor
//    private static class ArchiveSource implements InputSource {
//        ArchiveFile file;
//
//        @Override
//        public InputStream stream() throws IOException {
//            file.archive().seek(file.entry().getName());
//            return file.archive();
//        }
//    }
//
//    public static void write(@NonNull DockerImageArchive archive, @NonNull OutputStream outputStream) throws IOException {
//        @Value
//        class TarEntry {
//            TarArchiveEntry entry;
//            Supplier<InputStream> source;
//        }
//
//        List<TarEntry> entries = new LinkedList<>();
//        // fill
//        entries.sort(Comparator.comparing((TarEntry x) -> x.entry.getName()));
//
//        try (TarArchiveOutputStream tarStream = new TarArchiveOutputStream(outputStream)) {
//            for (TarEntry entry : entries) {
//
//entry.entry.setSize();
//            }
//        }
//
//        throw new UnsupportedOperationException();
//    }
}
