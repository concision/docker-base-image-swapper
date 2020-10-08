package me.concision.warcrimes.docker.swapper.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import me.concision.warcrimes.docker.swapper.util.io.RandomAccessTarArchiveFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Data
public class DockerImage {
    @ToString.Exclude
    protected DockerImageArchive imageArchive;

    // manifest[i]
    protected JsonObject manifestElementJson;

    // file: manifest[i].Config
    protected TarArchiveEntry configEntry;
    // manifest[i].Config
    protected DockerImageConfig config;
    // manifest[i].RepoTags
    protected List<String> repoTags = new ArrayList<>();
    // manifest[i].Layers
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
        DockerImage image = new DockerImage();

        image.imageArchive(imageArchive);
        image.manifestElementJson(manifestElementJsonObject);

        // manifest[i].Config
        {
            if (!manifestElementJsonObject.has("Config"))
                throw new RuntimeException("missing entry: Config");
            JsonElement configFileJsonElement = manifestElementJsonObject.get("Config");

            if (!configFileJsonElement.isJsonPrimitive())
                throw new RuntimeException("unexpected non-string entry: Config");
            JsonPrimitive configFileJsonPrimitive = configFileJsonElement.getAsJsonPrimitive();
            if (!configFileJsonPrimitive.isString())
                throw new RuntimeException("unexpected non-string entry: Config");

            String imageConfigPathString = configFileJsonPrimitive.getAsString();
            try {
                TarArchiveEntry imageConfigEntry = tarArchive.tryGetEntry(imageConfigPathString).getEntry();
                image.configEntry(imageConfigEntry);
                String imageConfigString = new String(tarArchive.readFullEntry(imageConfigEntry), StandardCharsets.ISO_8859_1);

                JsonObject imageConfigJsonObject;
                try {
                    imageConfigJsonObject = new Gson().fromJson(imageConfigString, JsonObject.class);
                } catch (JsonSyntaxException exception) {
                    throw new JsonSyntaxException("failed to parse configuration as a JSON object: " + imageConfigPathString, exception);
                }

                image.config(DockerImageConfig.from(imageConfigJsonObject));
            } catch (RuntimeException exception) {
                throw new RuntimeException("error reading configuration: Config", exception);
            }
        }

        // manifest[i].RepoTags
        {
            if (!manifestElementJsonObject.has("RepoTags"))
                throw new RuntimeException("missing entry: RepoTags");
            JsonElement repoTagsJsonElement = manifestElementJsonObject.get("RepoTags");
            if (!repoTagsJsonElement.isJsonArray())
                throw new RuntimeException("unexpected non-array entry: RepoTags");

            JsonArray repoTagsJsonArray = repoTagsJsonElement.getAsJsonArray();
            for (int t = 0; t < repoTagsJsonArray.size(); t++) {
                JsonElement repoTagElementJsonElement = repoTagsJsonArray.get(t);

                if (!repoTagElementJsonElement.isJsonPrimitive())
                    throw new RuntimeException("unexpected non-string entry: RepoTags[" + t + "]");
                JsonPrimitive repoTagElementJsonPrimitive = repoTagElementJsonElement.getAsJsonPrimitive();
                if (!repoTagElementJsonPrimitive.isString())
                    throw new RuntimeException("unexpected non-string entry: RepoTags[" + t + "]");

                image.addRepoTag(repoTagElementJsonPrimitive.getAsString());
            }
        }

        // read manifest[i].Layers
        {
            if (!manifestElementJsonObject.has("Layers"))
                throw new RuntimeException("missing entry: Layers");
            JsonElement layersJsonElement = manifestElementJsonObject.get("Layers");
            if (!layersJsonElement.isJsonArray())
                throw new RuntimeException("unexpected non-array entry: Layers");

            JsonArray layersJsonArray = layersJsonElement.getAsJsonArray();
            for (int l = 0; l < layersJsonArray.size(); l++) {
                JsonElement layerElementJsonElement = layersJsonArray.get(l);

                if (!layerElementJsonElement.isJsonPrimitive())
                    throw new RuntimeException("unexpected non-string entry: Layers[" + l + "]");
                JsonPrimitive layerElementJsonPrimitive = layerElementJsonElement.getAsJsonPrimitive();
                if (!layerElementJsonPrimitive.isString())
                    throw new RuntimeException("unexpected non-string entry: Layers[" + l + "]");

                String layerPath = layerElementJsonPrimitive.getAsString();
                if (layerPath.lastIndexOf('/') < 0)
                    throw new RuntimeException("layer has no parent directory: Layers[" + l + "]");

                try {
                    image.addLayer(DockerLayer.from(tarArchive, layerPath, l == 0, l == layersJsonArray.size() - 1));
                } catch (RuntimeException exception) {
                    throw new RuntimeException("error reading layer files: Layers[" + l + "]", exception);
                }
            }
        }

        return image;
    }
}
