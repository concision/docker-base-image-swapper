package me.concision.warcrimes.docker.swapper.transformers;

import com.google.gson.JsonObject;
import lombok.extern.log4j.Log4j2;
import me.concision.warcrimes.docker.swapper.api.DockerImage;
import me.concision.warcrimes.docker.swapper.api.DockerLayer;
import me.concision.warcrimes.docker.swapper.transformer.ImageState;
import me.concision.warcrimes.docker.swapper.transformer.ImageTransformer;
import me.concision.warcrimes.docker.swapper.util.Sha256Hash;

import java.util.stream.Collectors;

@Log4j2
public class T08NewConfigName implements ImageTransformer {
    @Override
    public void transform(ImageState state) {
        DockerImage outImage = state.outImage();

        outImage.configEntry().setName(
                Sha256Hash.hexHash(outImage.layers().stream().map(image -> image.rootDirectory().getName()).collect(Collectors.joining())) + ".json"
        );
    }
}
