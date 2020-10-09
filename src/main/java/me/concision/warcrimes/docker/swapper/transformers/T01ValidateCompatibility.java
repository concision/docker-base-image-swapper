package me.concision.warcrimes.docker.swapper.transformers;

import com.google.gson.JsonObject;
import me.concision.warcrimes.docker.swapper.api.DockerImage;
import me.concision.warcrimes.docker.swapper.api.DockerImageArchive;
import me.concision.warcrimes.docker.swapper.api.DockerImageConfig;
import me.concision.warcrimes.docker.swapper.api.DockerLayer;
import me.concision.warcrimes.docker.swapper.transformer.ImageState;
import me.concision.warcrimes.docker.swapper.transformer.ImageTransformer;

import java.util.ArrayList;
import java.util.List;

public class T01ValidateCompatibility implements ImageTransformer {
    @Override
    public void transform(ImageState state) {
        List<String> oldDiffs = state.oldImage().config().diffIds();
        List<String> inputDiffs = state.inputImage().config().diffIds();

        for (int i = 0; i < oldDiffs.size(); i++) {
            if (!oldDiffs.get(i).equals(inputDiffs.get(i))) {
                throw new RuntimeException("input image is not based off of the old base image");
            }
        }
    }
}
