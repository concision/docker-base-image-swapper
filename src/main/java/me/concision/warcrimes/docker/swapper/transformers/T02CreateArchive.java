package me.concision.warcrimes.docker.swapper.transformers;

import com.google.gson.JsonObject;
import me.concision.warcrimes.docker.swapper.api.DockerImage;
import me.concision.warcrimes.docker.swapper.api.DockerImageArchive;
import me.concision.warcrimes.docker.swapper.api.DockerImageConfig;
import me.concision.warcrimes.docker.swapper.transformer.ImageState;
import me.concision.warcrimes.docker.swapper.transformer.ImageTransformer;

import java.util.ArrayList;

public class T02CreateArchive implements ImageTransformer {
    @Override
    public void transform(ImageState state) {
        DockerImageArchive imageArchive = new DockerImageArchive();

        DockerImageArchive inputArchive = state.inputArchive();
        if (inputArchive.repositoryEntry() != null) {
            imageArchive.repositoryEntry(inputArchive.repositoryEntry());
            imageArchive.repositoryJson(new JsonObject());
        }
        imageArchive.manifestEntry(inputArchive.manifestEntry());

        DockerImage image = new DockerImage();

        DockerImage inputImage = state.inputImage();
        image.archive(imageArchive);

        image.manifestElementJson(inputImage.manifestElementJson().deepCopy());
        image.configEntry(inputImage.configEntry());
        DockerImageConfig config = DockerImageConfig.from(inputImage.config().json());
        config.history(new ArrayList<>());
        config.diffIds(new ArrayList<>());
        image.config(config);

        imageArchive.addImage(image);
        state.outArchive(imageArchive);
    }
}
