package me.concision.warcrimes.docker.swapper.api;

import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NonNull;

@Data
public class DockerContainerConfig {
    protected JsonObject json;

    static DockerContainerConfig from(@NonNull JsonObject configJson) {
        DockerContainerConfig config = new DockerContainerConfig();
        config.json(configJson);
        return config;
    }
}
