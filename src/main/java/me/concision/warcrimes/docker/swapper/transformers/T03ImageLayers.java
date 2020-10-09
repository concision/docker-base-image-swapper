package me.concision.warcrimes.docker.swapper.transformers;

import me.concision.warcrimes.docker.swapper.api.DockerImageConfig.HistoryRecord;
import me.concision.warcrimes.docker.swapper.api.DockerLayer;
import me.concision.warcrimes.docker.swapper.transformer.ImageState;
import me.concision.warcrimes.docker.swapper.transformer.ImageTransformer;

import java.util.ArrayList;
import java.util.List;

public class T03ImageLayers implements ImageTransformer {
    @Override
    public void transform(ImageState state) {
        // layers
        {
            List<DockerLayer> layers = new ArrayList<>();
            layers.addAll(state.newImage().layers());
            List<DockerLayer> inputLayers = state.inputImage().layers();
            for (int i = state.oldImage().layers().size(); i < inputLayers.size(); i++) {
                layers.add(inputLayers.get(i));
            }
            state.outImage().layers(layers);
        }

        // diff ids
        {
            List<String> diffIds = new ArrayList<>();
            diffIds.addAll(state.newImage().config().diffIds());
            List<String> inputDiffs = state.inputImage().config().diffIds();
            for (int i = state.oldImage().config().diffIds().size(); i < inputDiffs.size(); i++) {
                diffIds.add(inputDiffs.get(i));
            }
            state.outImage().config().diffIds(diffIds);
        }
        // history
        {
            List<HistoryRecord> history = new ArrayList<>();
            history.addAll(state.newImage().config().history());
            List<HistoryRecord> inputHistory = state.inputImage().config().history();
            for (int i = state.oldImage().config().history().size(); i < inputHistory.size(); i++) {
                history.add(inputHistory.get(i));
            }
            state.outImage().config().history(history);
        }
    }
}
