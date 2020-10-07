package me.concision.warcrimes.docker.swapper.archive;

import lombok.Value;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

@Value
public class DockerImage {
    JSONArray manifest;
    List<SubImage> images;
    Map<String, byte[]> files;

    @Value
    public static class SubImage {
        String configurationName;
        JSONObject configuration;
        List<ImageLayer> layers;
    }

    @Value
    public static class ImageLayer {
        String root;
        JSONObject manifest;
        Map<String, byte[]> files;
    }
}
