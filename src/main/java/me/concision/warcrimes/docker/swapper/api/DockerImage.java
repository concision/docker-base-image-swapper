package me.concision.warcrimes.docker.swapper.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import me.concision.warcrimes.docker.swapper.util.io.RandomAccessTarArchiveFile;
import me.concision.warcrimes.docker.swapper.util.io.RandomAccessTarArchiveFile.ArchiveEntryOffset;
import me.concision.warcrimes.docker.swapper.util.io.RandomAccessTarArchiveFile.ArchiveFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Data
public class DockerImage {
    protected DockerImageArchive imageArchive;

    // original manifest entry object element
    protected JsonObject manifestElementJson;

    protected TarArchiveEntry configEntry;
    // processed 'manifest[i].Config'
    protected DockerImageConfiguration config;
    // extracted 'manifest[i].RepoTags'
    protected List<String> repoTags = new ArrayList<>();
    // processed 'manifest[i].Layers'
    protected List<DockerLayer> layers = new ArrayList<>();


    public void addRepoTag(@NonNull String repoTag) {
        repoTags.add(repoTag);
    }

    public void addLayer(@NonNull DockerLayer layer) {
        layers.add(layer);
    }


    static DockerImage from(
            @NonNull RandomAccessTarArchiveFile tarArchive,
            @NonNull DockerImageArchive imageArchive,
            @NonNull JsonObject manifestElementJsonObject
    ) throws IOException {
        // create new docker image for element
        DockerImage image = new DockerImage();
        image.imageArchive(imageArchive);
        image.manifestElementJson(manifestElementJsonObject);

        // read manifest[i].Config
        {
            // read configuration
            JsonElement configFileJsonElement = manifestElementJsonObject.get("Config");

            // validate configuration string
            if (!configFileJsonElement.isJsonPrimitive())
                throw new RuntimeException("unexpected non-string entry: Config");
            JsonPrimitive configFileJsonPrimitive = configFileJsonElement.getAsJsonPrimitive();
            if (!configFileJsonPrimitive.isString())
                throw new RuntimeException("unexpected non-string entry: Config");

            // parse configuration string
            String configPathString = configFileJsonPrimitive.getAsString();
            try {
                // read configuration
                TarArchiveEntry configEntry = tarArchive.tryGetEntry(configPathString).getEntry();
                image.configEntry(configEntry);
                String configString = new String(tarArchive.readFullEntry(configEntry), StandardCharsets.ISO_8859_1);

                // validate configuration
                JsonObject configJsonObject;
                try {
                    configJsonObject = new Gson().fromJson(configString, JsonObject.class);
                } catch (JsonSyntaxException exception) {
                    throw new JsonSyntaxException("failed to parse configuration as a JSON object: " + configPathString, exception);
                }

                // parse configuration
                image.config(DockerImageConfiguration.from(configJsonObject));
            } catch (RuntimeException exception) {
                throw new RuntimeException("error reading configuration: Config", exception);
            }
        }

        // read manifest[i].RepoTags
        {
            JsonElement repoTagsJsonElement = manifestElementJsonObject.get("RepoTags");
            // validate repo tags
            if (!repoTagsJsonElement.isJsonArray())
                throw new RuntimeException("unexpected non-array entry: RepoTags");
            // read repo tags
            JsonArray repoTagsJsonArray = repoTagsJsonElement.getAsJsonArray();
            for (int t = 0; t < repoTagsJsonArray.size(); t++) {
                // read repo tag
                JsonElement repoTagElementJsonElement = repoTagsJsonArray.get(t);

                // validate repo tag
                if (!repoTagElementJsonElement.isJsonPrimitive())
                    throw new RuntimeException("unexpected non-string entry: RepoTags[" + t + "]");
                JsonPrimitive repoTagElementJsonPrimitive = repoTagElementJsonElement.getAsJsonPrimitive();
                if (!repoTagElementJsonPrimitive.isString())
                    throw new RuntimeException("unexpected non-string entry: RepoTags[" + t + "]");

                // addend repo tag
                image.addRepoTag(repoTagElementJsonPrimitive.getAsString());
            }
        }

        // read manifest[i].Layers
        {
            JsonElement layersJsonElement = manifestElementJsonObject.get("Layers");
            // validate layers
            if (!layersJsonElement.isJsonArray())
                throw new RuntimeException("unexpected non-array entry: Layers");

            // read layers
            JsonArray layersJsonArray = layersJsonElement.getAsJsonArray();
            for (int l = 0; l < layersJsonArray.size(); l++) {
                // read layer
                JsonElement layerElementJsonElement = layersJsonArray.get(l);

                // validate layer
                if (!layerElementJsonElement.isJsonPrimitive())
                    throw new RuntimeException("unexpected non-string entry: Layers[" + l + "]");
                JsonPrimitive layerElementJsonPrimitive = layerElementJsonElement.getAsJsonPrimitive();
                if (!layerElementJsonPrimitive.isString())
                    throw new RuntimeException("unexpected non-string entry: Layers[" + l + "]");

                // read layer path
                String layerPath = layerElementJsonPrimitive.getAsString();
                if (layerPath.lastIndexOf('/') < 0)
                    throw new RuntimeException("layer has no parent directory: Layers[" + l + "]");
                // get layer root directory
                String layerRootPath = layerPath.substring(0, layerPath.lastIndexOf('/') + 1);

                // read layer
                DockerLayer layer = new DockerLayer();
                try {
                    // get tar entry for layer root directory
                    layer.rootDirectory(tarArchive.tryGetEntry(layerRootPath).getEntry());
                    // ensure layer file exists
                    tarArchive.tryGetEntry(layerPath);

                    // get layer configuration
                    TarArchiveEntry layerManifestEntry = tarArchive.tryGetEntry(layerRootPath + "json").getEntry();
                    image.configEntry(layerManifestEntry);
                    try {
                        // read layer configuration
                        String manifestString = new String(tarArchive.readFullEntry(layerManifestEntry), StandardCharsets.ISO_8859_1);
                        JsonObject manifestJsonObject;
                        try {
                            manifestJsonObject = new Gson().fromJson(manifestString, JsonObject.class);
                        } catch (JsonSyntaxException exception) {
                            throw new JsonSyntaxException("failed to parse file as a JSON object", exception);
                        }
                        // parse manifest
                        layer.manifestEntry(layerManifestEntry);
                        layer.manifest(DockerLayerManifest.from(manifestJsonObject));
                    } catch (RuntimeException exception) {
                        throw new RuntimeException("error reading layer configuration: " + layerManifestEntry.getName(), exception);
                    }

                    // read all layer files
                    for (ArchiveEntryOffset entry : tarArchive) {
                        if (entry.getName().startsWith(layerRootPath)) {
                            layer.addFile(new ArchiveFile(tarArchive, entry));
                        }
                    }
                } catch (RuntimeException exception) {
                    throw new RuntimeException("error reading layer files: Layers[" + l + "]", exception);
                }

                // add layer
                image.addLayer(layer);
            }
        }

        return image;
    }
}
