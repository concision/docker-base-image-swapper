package me.concision.warcrimes.docker.swapper.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.Data;
import lombok.NonNull;

@Data
public class DockerLayerManifest {
    protected JsonObject json;

    protected DockerContainerConfig containerConfig;
    protected DockerContainerConfig config;


    public String id() {
        return json.get("id").getAsString();
    }

    public void id(@NonNull String parent) {
        json.addProperty("id", parent);
    }

    public String parent() {
        return json.get("parent").getAsString();
    }

    public void parent(@NonNull String id) {
        json.addProperty("parent", id);
    }

    public String architecture() {
        return json.get("architecture").getAsString();
    }

    public void architecture(@NonNull String architecture) {
        json.addProperty("architecture", architecture);
    }

    public String os() {
        return json.get("os").getAsString();
    }

    public void os(@NonNull String os) {
        json.addProperty("os", os);
    }


    public static DockerLayerManifest from(@NonNull JsonObject manifestJsonObject, boolean isFirstLayer, boolean isLastLayer) {
        DockerLayerManifest layerManifest = new DockerLayerManifest();
        layerManifest.json(manifestJsonObject);

        // architecture, os
        for (String key : new String[]{"id", "parent"}) {
            if (isFirstLayer && key.equals("parent")) continue;
            if (!manifestJsonObject.has(key))
                throw new RuntimeException("missing entry: " + key);
            JsonElement jsonElement = manifestJsonObject.get(key);
            if (!jsonElement.isJsonPrimitive())
                throw new RuntimeException("unexpected non-string entry: " + key);
            JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();
            if (!jsonPrimitive.isString())
                throw new RuntimeException("unexpected non-string entry: " + key);
        }

        // container_config
        {
            if (!manifestJsonObject.has("container_config"))
                throw new RuntimeException("missing entry: container_config");
            JsonElement containerConfigJsonElement = manifestJsonObject.get("container_config");
            if (!containerConfigJsonElement.isJsonObject())
                throw new RuntimeException("unexpected non-object entry: container_config");
            JsonObject containerConfigJsonObject = containerConfigJsonElement.getAsJsonObject();

            try {
                layerManifest.containerConfig(DockerContainerConfig.from(containerConfigJsonObject));
            } catch (RuntimeException exception) {
                throw new RuntimeException("error reading configuration: container_config", exception);
            }
        }

        // config
        if (isLastLayer) {
            if (!manifestJsonObject.has("config"))
                throw new RuntimeException("missing entry: config");
            JsonElement configJsonElement = manifestJsonObject.get("config");
            if (!configJsonElement.isJsonObject())
                throw new RuntimeException("unexpected non-object entry: config");
            JsonObject configJsonObject = configJsonElement.getAsJsonObject();

            try {
                layerManifest.config(DockerContainerConfig.from(configJsonObject));
            } catch (RuntimeException exception) {
                throw new RuntimeException("error reading configuration: config", exception);
            }
        }

        return layerManifest;
    }
}
