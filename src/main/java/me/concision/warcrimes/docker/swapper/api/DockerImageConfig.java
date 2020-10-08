package me.concision.warcrimes.docker.swapper.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.Data;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

@Data
public class DockerImageConfig {
    protected JsonObject json;

    protected DockerContainerConfig containerConfig;
    protected DockerContainerConfig config;

    protected List<HistoryRecord> history = new ArrayList<>();

    protected List<String> diffIds = new ArrayList<>();


    public void addHistory(@NonNull HistoryRecord historyRecord) {
        history.add(historyRecord);
    }

    public void addDiffId(@NonNull String diffId) {
        diffIds.add(diffId);
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


    public static DockerImageConfig from(@NonNull JsonObject imageConfigJsonObject) {
        DockerImageConfig imageConfig = new DockerImageConfig();
        imageConfig.json(imageConfigJsonObject);

        // architecture, os
        for (String key : new String[]{"architecture", "os"}) {
            if (!imageConfigJsonObject.has(key))
                throw new RuntimeException("missing entry: " + key);
            JsonElement jsonElement = imageConfigJsonObject.get(key);
            if (!jsonElement.isJsonPrimitive())
                throw new RuntimeException("unexpected non-string entry: " + key);
            JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();
            if (!jsonPrimitive.isString())
                throw new RuntimeException("unexpected non-string entry: " + key);
        }

        // container_config
        {
            if (!imageConfigJsonObject.has("container_config"))
                throw new RuntimeException("missing entry: container_config");
            JsonElement containerConfigJsonElement = imageConfigJsonObject.get("container_config");
            if (!containerConfigJsonElement.isJsonObject())
                throw new RuntimeException("unexpected non-object entry: container_config");
            JsonObject containerConfigJsonObject = containerConfigJsonElement.getAsJsonObject();

            try {
                imageConfig.containerConfig(DockerContainerConfig.from(containerConfigJsonObject));
            } catch (RuntimeException exception) {
                throw new RuntimeException("error reading configuration: container_config", exception);
            }
        }

        // config
        {
            if (!imageConfigJsonObject.has("config"))
                throw new RuntimeException("missing entry: config");
            JsonElement configJsonElement = imageConfigJsonObject.get("config");
            if (!configJsonElement.isJsonObject())
                throw new RuntimeException("unexpected non-object entry: config");
            JsonObject configJsonObject = configJsonElement.getAsJsonObject();

            try {
                imageConfig.config(DockerContainerConfig.from(configJsonObject));
            } catch (RuntimeException exception) {
                throw new RuntimeException("error reading configuration: config", exception);
            }
        }

        // history
        {
            if (!imageConfigJsonObject.has("history"))
                throw new RuntimeException("missing entry: history");
            JsonElement historyJsonElement = imageConfigJsonObject.get("history");
            if (!historyJsonElement.isJsonArray())
                throw new RuntimeException("unexpected non-array entry: history");
            JsonArray historyJsonArray = historyJsonElement.getAsJsonArray();

            for (int h = 0; h < historyJsonArray.size(); h++) {
                JsonElement historyElementJsonElement = historyJsonArray.get(h);
                if (!historyElementJsonElement.isJsonObject())
                    throw new RuntimeException("unexpected non-object element in array: history[" + h + "]");
                JsonObject historyElementJsonObject = historyElementJsonElement.getAsJsonObject();

                try {
                    imageConfig.addHistory(HistoryRecord.from(historyElementJsonObject));
                } catch (RuntimeException exception) {
                    throw new RuntimeException("error reading configuration: history[" + h + "]", exception);
                }
            }
        }

        // rootfs
        {
            if (!imageConfigJsonObject.has("rootfs"))
                throw new RuntimeException("missing entry: rootfs");
            JsonElement rootfsJsonElement = imageConfigJsonObject.get("rootfs");
            if (!rootfsJsonElement.isJsonObject())
                throw new RuntimeException("unexpected non-object entry: rootfs");
            JsonObject rootfsJsonObject = rootfsJsonElement.getAsJsonObject();
            if (!rootfsJsonObject.has("type"))
                throw new RuntimeException("unexpected non-string entry: rootfs.type");
            JsonElement rootfsTypeJsonElement = rootfsJsonObject.get("type");
            if (!rootfsTypeJsonElement.isJsonPrimitive())
                throw new RuntimeException("unexpected non-string entry: rootfs.type");
            JsonPrimitive rootfsTypeJsonPrimitive = rootfsTypeJsonElement.getAsJsonPrimitive();
            if (!rootfsTypeJsonPrimitive.isString())
                throw new RuntimeException("unexpected non-string entry: rootfs.type");
            if (!"layers".equals(rootfsTypeJsonPrimitive.getAsString()))
                throw new RuntimeException("invalid value; expected \"layers\": rootfs.type");

            if (!rootfsJsonObject.has("diff_ids"))
                throw new RuntimeException("missing entry: rootfs.diff_ids");
            JsonElement diffIdsJsonElement = rootfsJsonObject.get("diff_ids");
            if (!diffIdsJsonElement.isJsonArray())
                throw new RuntimeException("unexpected non-array entry: rootfs.diff_ids");
            JsonArray diffIdsJsonArray = diffIdsJsonElement.getAsJsonArray();

            for (int d = 0; d < diffIdsJsonArray.size(); d++) {
                JsonElement diffIdElementJsonElement = diffIdsJsonArray.get(d);
                if (!diffIdElementJsonElement.isJsonPrimitive())
                    throw new RuntimeException("unexpected non-string element in array: rootfs.diff_ids[" + d + "]");
                JsonPrimitive diffIdElementJsonPrimitive = diffIdElementJsonElement.getAsJsonPrimitive();
                if (!diffIdElementJsonPrimitive.isString())
                    throw new RuntimeException("unexpected non-string element in array: rootfs.diff_ids[" + d + "]");

                imageConfig.addDiffId(diffIdElementJsonPrimitive.getAsString());
            }
        }

        return imageConfig;
    }

    @Data
    public static class HistoryRecord {
        // history[i]: raw record
        private JsonObject json;

        static HistoryRecord from(@NonNull JsonObject historyJsonObject) {
            HistoryRecord historyRecord = new HistoryRecord();
            historyRecord.json(historyJsonObject);

            if (!historyJsonObject.has("created_by"))
                throw new RuntimeException("missing entry: created_by");
            JsonElement jsonElement = historyJsonObject.get("created_by");
            if (!jsonElement.isJsonPrimitive())
                throw new RuntimeException("unexpected non-string entry: created_by");
            JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();
            if (!jsonPrimitive.isString())
                throw new RuntimeException("unexpected non-string entry: created_by");

            return historyRecord;
        }
    }
}
