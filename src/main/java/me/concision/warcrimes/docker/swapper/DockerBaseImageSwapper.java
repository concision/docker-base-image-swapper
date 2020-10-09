package me.concision.warcrimes.docker.swapper;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import me.concision.warcrimes.docker.swapper.api.DockerImage;
import me.concision.warcrimes.docker.swapper.api.DockerImageArchive;
import me.concision.warcrimes.docker.swapper.transformer.ImageState;
import me.concision.warcrimes.docker.swapper.transformer.ImageTransformer;
import me.concision.warcrimes.docker.swapper.transformers.T01ValidateCompatibility;
import me.concision.warcrimes.docker.swapper.transformers.T02CreateArchive;
import me.concision.warcrimes.docker.swapper.transformers.T03ImageLayers;
import me.concision.warcrimes.docker.swapper.transformers.T04RelinkLastLayerConfig;
import me.concision.warcrimes.docker.swapper.transformers.T05RelinkLayerIds;
import me.concision.warcrimes.docker.swapper.transformers.T07LinkOutputTag;
import me.concision.warcrimes.docker.swapper.transformers.T08NewConfigName;
import me.concision.warcrimes.docker.swapper.util.io.RandomAccessTarArchiveFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RequiredArgsConstructor
public class DockerBaseImageSwapper {
    @Getter
    @NonNull
    protected final CommandArguments args;

    protected final List<RandomAccessTarArchiveFile> archives = new ArrayList<>();
    protected final List<DockerImageArchive> imageArchives = new ArrayList<>();

    private ImageState imageState;

    public void execute() {
        try {
            this.index();
            this.parseImages();
            this.scanImageTags();
            this.swapBaseImages();
        } finally {
            this.cleanup();
        }
    }

    private void index() {
        // check file exists and subsequently acquire file locks
        for (File archiveFile : args.archiveFiles) {
            log.info("Acquiring image file lock: {}", archiveFile.getAbsoluteFile());
            try {
                this.archives.add(new RandomAccessTarArchiveFile(archiveFile));
            } catch (Throwable throwable) {
                throw new RuntimeException("failed to acquire image file lock: " + archiveFile.getAbsolutePath(), throwable);
            }
        }
        // index archives
        for (RandomAccessTarArchiveFile archive : archives) {
            log.info("Building indexes from .tar archive: {}", archive.getFile().getAbsoluteFile());
            try {
                archive.createIndex();
            } catch (Throwable throwable) {
                throw new RuntimeException("failed to acquire image file lock: " + archive.getFile().getAbsolutePath(), throwable);
            }
        }
    }

    private void parseImages() {
        // parse archives
        log.info("Parsing images archive structures");
        for (RandomAccessTarArchiveFile archive : archives) {
            try {
                DockerImageArchive imageArchive = DockerImageArchive.from(archive);
                log.info(
                        "Discovered image tags: {}",
                        imageArchive.images().stream().map(DockerImage::repoTags).collect(Collectors.toList()).toString()
                );
                this.imageArchives.add(imageArchive);
            } catch (Throwable throwable) {
                throw new RuntimeException("failed to read image configuration: " + archive.getFile().getAbsolutePath(), throwable);
            }
        }
    }

    private void scanImageTags() {
        log.info("Scanning image archives for tags: [" + args.oldBaseImageName + ", " + args.newBaseImageName + ", " + args.inputImageName + "]");

        // grab old new
        DockerImage oldImage;
        DockerImage newImage;
        DockerImage inputImage;
        try {
            oldImage = this.imageArchives.stream().flatMap(archive -> archive.images().stream())
                    .filter(image -> image.repoTags().contains(args.oldBaseImageName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("old base image not found: " + args.oldBaseImageName));
            newImage = this.imageArchives.stream().flatMap(archive -> archive.images().stream())
                    .filter(image -> image.repoTags().contains(args.newBaseImageName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("new base image tag not found: " + args.newBaseImageName));
            inputImage = this.imageArchives.stream().flatMap(archive -> archive.images().stream())
                    .filter(image -> image.repoTags().contains(args.inputImageName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("old base image not found: " + args.inputImageName));
        } catch (RuntimeException throwable) {
            throw new RuntimeException("failed to find an image tag", throwable);
        }

        this.imageState = new ImageState(args, oldImage, newImage, inputImage);
        log.info(oldImage);
        log.info(newImage);
        log.info(inputImage);
    }

    private void swapBaseImages() {
        log.info("Computing virtual transformation of input image with swapped base image");
        for (ImageTransformer transformer : new ImageTransformer[]{
                // check compatibility of old and app image
                new T01ValidateCompatibility(),
                // create new archive and image
                new T02CreateArchive(),
                // add new image and app image layers
                new T03ImageLayers(),
                // relink last layer of new base image (if necessary)
                new T04RelinkLastLayerConfig(),
                // change ids of later layers of app image, relink parents; update internal director hashes deterministically
                new T05RelinkLayerIds(),
                // reconcile image configuration state of last layer + image layer

                // recompile manifest with new tag, config, layers
                new T07LinkOutputTag(),
                // change config seed with deterministic hash
                new T08NewConfigName(),
        }) {
            try {
                transformer.transform(this.imageState);
            } catch (Throwable throwable) {
                throw new RuntimeException("failed image transformation at transformer: " + transformer.getClass().getName(), throwable);
            }
        }

        log.info("Writing transformed image");
        try {
            DockerImageArchive.write(this.imageState.outArchive(), new BufferedOutputStream(new FileOutputStream(args.outputImageFile)));
        } catch (Throwable throwable) {
            log.fatal("Failed writing new swapped image archive", throwable);
        }
    }

    private void cleanup() {
        for (RandomAccessTarArchiveFile archive : archives) {
            try {
                archive.close();
            } catch (IOException ignored) {
            }
        }
        archives.clear();
        imageArchives.clear();
    }
}
