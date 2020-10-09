package me.concision.warcrimes.docker.swapper.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import me.concision.warcrimes.docker.swapper.api.DockerImageConfig.HistoryRecord;
import me.concision.warcrimes.docker.swapper.util.io.RandomAccessTarArchiveFile;
import me.concision.warcrimes.docker.swapper.util.io.RandomAccessTarArchiveFile.ArchiveEntryOffset;
import me.concision.warcrimes.docker.swapper.util.io.RandomAccessTarArchiveFile.ArchiveFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
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


    // TODO: write an RFC-compliant JSON library that does not suck
    public static DockerImageArchive from(@NonNull RandomAccessTarArchiveFile tarArchive) throws IOException {
        DockerImageArchive imageArchive = new DockerImageArchive();

        // file: repositories
        ArchiveEntryOffset repositoriesOffset = tarArchive.getEntry("repositories");
        if (repositoriesOffset != null) {
            imageArchive.repositoryEntry(repositoriesOffset.getEntry());

            String repositoryString = new String(tarArchive.readFullEntry(repositoriesOffset));
            JsonObject repositoryJson;
            try {
                repositoryJson = new Gson().fromJson(repositoryString, JsonObject.class);
            } catch (JsonSyntaxException exception) {
                throw new JsonSyntaxException("failed to parse 'repositories' file as a JSON object", exception);
            }
            imageArchive.repositoryJson(repositoryJson);
        }

        // file: manifest.json
        try {
            TarArchiveEntry manifestEntry = tarArchive.tryGetEntry("manifest.json").getEntry();
            imageArchive.manifestEntry(manifestEntry);

            String manifestString = new String(tarArchive.readFullEntry(manifestEntry));
            JsonArray manifestJsonArray;
            try {
                manifestJsonArray = new Gson().fromJson(manifestString, JsonArray.class);
            } catch (JsonSyntaxException exception) {
                throw new JsonSyntaxException("failed to parse file as a JSON array", exception);
            }

            // manifest[i]
            for (int i = 0; i < manifestJsonArray.size(); i++) {
                JsonElement manifestElementJsonElement = manifestJsonArray.get(i);
                if (!manifestElementJsonElement.isJsonObject())
                    throw new RuntimeException("unexpected non-object element in array: [" + i + "]");
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

    private interface InputSource {
        InputStream stream(TarArchiveEntry entry) throws IOException;
    }

    @RequiredArgsConstructor
    private static class ByteSource implements InputSource {
        private final byte[] bytes;

        @Override
        public InputStream stream(TarArchiveEntry entry) {
            entry.setSize(bytes.length);
            return new ByteArrayInputStream(bytes);
        }
    }

    @RequiredArgsConstructor
    private static class ArchiveSource implements InputSource {
        private final ArchiveFile file;

        @Override
        public InputStream stream(TarArchiveEntry entry) throws IOException {
            TarArchiveEntry seek = file.archive().seek(file.entry().getName());
            entry.setSize(seek.getSize());
            return file.archive();
        }
    }

    public static void write(@NonNull DockerImageArchive archive, @NonNull OutputStream outputStream) throws IOException {
        @Value
        class TarEntry {
            TarArchiveEntry entry;
            InputSource supplier;
        }

        List<TarEntry> entries = new LinkedList<>();

        // file: repositories
        if (archive.repositoryEntry() != null) {
            entries.add(new TarEntry(
                    archive.repositoryEntry(),
                    new ByteSource(archive.repositoryJson.toString().getBytes(StandardCharsets.ISO_8859_1))
            ));
        }

        // file: manifest.json
        JsonArray manifestArray = new JsonArray();
        for (DockerImage image : archive.images()) {
            JsonObject imageManifest = new JsonObject();
            imageManifest.addProperty("Config", image.configEntry.getName());
            JsonArray repoTags = new JsonArray();
            for (String repoTag : image.repoTags()) {
                repoTags.add(repoTag);
            }
            imageManifest.add("RepoTags", repoTags);
            JsonArray layerArchives = new JsonArray();
            for (DockerLayer layer : image.layers()) {
                layerArchives.add(layer.archiveEntry.getName());
            }
            imageManifest.add("Layers", layerArchives);
            manifestArray.add(imageManifest);
        }
        entries.add(new TarEntry(
                archive.manifestEntry(),
                new ByteSource(manifestArray.toString().getBytes(StandardCharsets.ISO_8859_1))
        ));


        for (DockerImage image : archive.images()) {
            for (DockerLayer layer : image.layers()) {
                DockerLayerManifest manifest = layer.manifest;
                JsonObject manifestJson = manifest.json.deepCopy();
                if (manifest.config != null) {
                    manifestJson.add("config", manifest.config.json());
                }
                manifestJson.add("container_config", manifest.containerConfig.json());
                entries.add(new TarEntry(
                        layer.manifestEntry,
                        new ByteSource(manifestJson.toString().getBytes(StandardCharsets.ISO_8859_1))
                ));

                for (ArchiveFile file : layer.files) {
                    entries.add(new TarEntry(file.entry().getEntry(), new ArchiveSource(file)));
                }
            }

            DockerImageConfig config = image.config;
            JsonObject configJson = config.json.deepCopy();
            configJson.add("config", config.config.json());
            configJson.add("container_config", config.containerConfig.json());
            JsonArray history = new JsonArray();
            for (HistoryRecord historyRecord : config.history) {
                history.add(historyRecord.json());
            }
            configJson.add("history", history);
            JsonObject rootfsObject = new JsonObject();
            configJson.add("rootfs", rootfsObject);
            rootfsObject.addProperty("type", "layers");
            JsonArray diffIdsArray = new JsonArray();
            for (String diffId : config.diffIds()) {
                diffIdsArray.add(diffId);
            }
            rootfsObject.add("diff_ids", diffIdsArray);

            entries.add(new TarEntry(
                    image.configEntry,
                    new ByteSource(configJson.toString().getBytes(StandardCharsets.ISO_8859_1))
            ));
        }

        // write
        entries.sort(Comparator.comparing((TarEntry entry) -> entry.entry.getName()));
        try (TarArchiveOutputStream tarStream = new TarArchiveOutputStream(outputStream)) {
            for (TarEntry entry : entries) {
                InputStream stream = entry.supplier.stream(entry.entry);
                tarStream.putArchiveEntry(entry.entry);
                IOUtils.copy(stream, tarStream);
                tarStream.closeArchiveEntry();
            }
        }
    }
}
