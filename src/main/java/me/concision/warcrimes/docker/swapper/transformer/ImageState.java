package me.concision.warcrimes.docker.swapper.transformer;

import lombok.Data;
import me.concision.warcrimes.docker.swapper.CommandArguments;
import me.concision.warcrimes.docker.swapper.api.DockerImage;
import me.concision.warcrimes.docker.swapper.api.DockerImageArchive;

@Data
public class ImageState {
    private final CommandArguments arguments;

    private final DockerImage oldImage;
    private final DockerImage newImage;
    private final DockerImage inputImage;

    private DockerImageArchive outArchive;

    public DockerImageArchive oldArchive() {
        return oldImage.archive();
    }

    public DockerImageArchive inputArchive() {
        return inputImage.archive();
    }

    public DockerImageArchive newArchive() {
        return newImage.archive();
    }

    public DockerImage outImage() {
        return outArchive.images().get(0);
    }
}
