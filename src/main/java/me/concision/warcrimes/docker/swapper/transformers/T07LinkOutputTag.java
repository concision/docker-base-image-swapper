package me.concision.warcrimes.docker.swapper.transformers;

import com.google.gson.JsonObject;
import lombok.extern.log4j.Log4j2;
import me.concision.warcrimes.docker.swapper.api.DockerImage;
import me.concision.warcrimes.docker.swapper.api.DockerLayer;
import me.concision.warcrimes.docker.swapper.transformer.ImageState;
import me.concision.warcrimes.docker.swapper.transformer.ImageTransformer;

@Log4j2
public class T07LinkOutputTag implements ImageTransformer {
    @Override
    public void transform(ImageState state) {
        DockerImage outImage = state.outImage();

        String outputImageName = state.arguments().outputImageName;
        outImage.addRepoTag(outputImageName);
        JsonObject repositoryJson = outImage.archive().repositoryJson();

        String name = outputImageName;
        String tag ="latest";
        if (outputImageName.contains(":")) {
            name = outputImageName.substring(0, outputImageName.indexOf(':'));
            tag = outputImageName.substring(outputImageName.indexOf(':') + 1);
        }

        JsonObject jsonObject1 = new JsonObject();
        DockerLayer lastLayer = outImage.layers().get(outImage.layers().size() - 1);
        jsonObject1.addProperty(tag, lastLayer.rootDirectory().getName().substring(0, lastLayer.rootDirectory().getName().indexOf('/')));

        repositoryJson.add(name, jsonObject1);
    }
}
