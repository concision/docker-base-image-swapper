package me.concision.warcrimes.docker.swapper.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.Data;
import lombok.NonNull;
import me.concision.warcrimes.docker.swapper.util.io.RandomAccessTarArchiveFile;
import me.concision.warcrimes.docker.swapper.util.io.RandomAccessTarArchiveFile.ArchiveEntryOffset;
import me.concision.warcrimes.docker.swapper.util.io.RandomAccessTarArchiveFile.ArchiveFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Data
public class DockerLayer {
    protected TarArchiveEntry rootDirectory;
    protected TarArchiveEntry archiveEntry;

    protected TarArchiveEntry manifestEntry;
    // resolved and processed manifest[i].Layers json configurations
    protected DockerLayerManifest manifest;

    // processed manifest[i].Layers: all files from layer directory, including configuration
    protected List<ArchiveFile> files = new ArrayList<>();


    public void addFile(@NonNull ArchiveFile file) {
        files.add(file);
    }


    static DockerLayer from(@NonNull RandomAccessTarArchiveFile tarArchive, @NonNull String layerPath, boolean isFirstLayer, boolean isLastLayer) throws IOException {
        DockerLayer layer = new DockerLayer();

        // get layer archive tar entry
        layer.archiveEntry(tarArchive.tryGetEntry(layerPath).getEntry());

        // validate layer has parent directory
        if (layerPath.lastIndexOf('/') < 0)
            throw new RuntimeException("layer has no parent directory: " + layerPath);
        // get layer root directory
        String layerRootPath = layerPath.substring(0, layerPath.lastIndexOf('/') + 1);

        // get tar entry for layer root directory
        layer.rootDirectory(tarArchive.tryGetEntry(layerRootPath).getEntry());
        // ensure layer file exists
        tarArchive.tryGetEntry(layerPath);

        // read layer manifest
        try {
            // read layer manifest
            TarArchiveEntry layerManifestEntry = tarArchive.tryGetEntry(layerRootPath + "json").getEntry();
            layer.manifestEntry(layerManifestEntry);
            String manifestString = new String(tarArchive.readFullEntry(layerManifestEntry), StandardCharsets.UTF_8);

            // validate manifest
            JsonObject manifestJsonObject;
            try {
                manifestJsonObject = new Gson().fromJson(manifestString, JsonObject.class);
            } catch (JsonSyntaxException exception) {
                throw new JsonSyntaxException("failed to parse file as a JSON object", exception);
            }

            // parse manifest
            layer.manifest(DockerLayerManifest.from(manifestJsonObject, isFirstLayer, isLastLayer));
        } catch (RuntimeException exception) {
            throw new RuntimeException("error reading layer configuration: " + layerRootPath + "json", exception);
        }

        // read all layer files
        for (ArchiveEntryOffset entry : tarArchive) {
            if (entry.getName().startsWith(layerRootPath)) {
                // ignore manifest
                if (!entry.getName().equals(layerRootPath + "json")) {
                    layer.addFile(new ArchiveFile(tarArchive, entry));
                }
            }
        }

        return layer;
    }
}
