package me.concision.warcrimes.docker.swapper.transformers;

import com.rits.cloning.Cloner;
import lombok.extern.log4j.Log4j2;
import me.concision.warcrimes.docker.swapper.api.DockerImage;
import me.concision.warcrimes.docker.swapper.api.DockerLayer;
import me.concision.warcrimes.docker.swapper.api.DockerLayerManifest;
import me.concision.warcrimes.docker.swapper.transformer.ImageState;
import me.concision.warcrimes.docker.swapper.transformer.ImageTransformer;
import me.concision.warcrimes.docker.swapper.util.Sha256Hash;
import me.concision.warcrimes.docker.swapper.util.io.RandomAccessTarArchiveFile.ArchiveFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
public class T05RelinkLayerIds implements ImageTransformer {
    @Override
    public void transform(ImageState state) {
        DockerImage oldImage = state.oldImage();
        if (oldImage.layers().size() < state.inputImage().layers().size()) {
            List<DockerLayer> outLayers = state.outImage().layers();
            for (int i = oldImage.layers().size() - 1; i < outLayers.size(); i++) {
                DockerLayer layer = outLayers.get(i);
                DockerLayerManifest manifest = layer.manifest();

                DockerLayer parent = null;
                if (0 <= i - 1) {
                    parent = outLayers.get(i - 1);
                }

                // compute new internal layer id
                manifest.id(Sha256Hash.hexHash((parent != null ? parent.manifest().id() : "") + manifest.id()));

                String root = Sha256Hash.hexHash(manifest.id()) + "/";

                TarArchiveEntry newArchiveEntry = new Cloner().deepClone(layer.archiveEntry());
                newArchiveEntry.setName(newArchiveEntry.getName().replace(layer.rootDirectory().getName(), root));
                layer.archiveEntry(newArchiveEntry);
                layer.manifestEntry().setName(layer.manifestEntry().getName().replace(layer.rootDirectory().getName(), root));

                Map<String, ArchiveFile> newFiles = new HashMap<>();
                for (Map.Entry<String, ArchiveFile> entry : layer.files().entrySet()) {
                    newFiles.put(entry.getKey().replace(layer.rootDirectory().getName(), root), entry.getValue());
                }
                layer.files(newFiles);

                layer.rootDirectory().setName(root);

                // update parent
                if (parent != null) {
                    manifest.parent(parent.manifest().id());
                }
            }
        }
    }
}
