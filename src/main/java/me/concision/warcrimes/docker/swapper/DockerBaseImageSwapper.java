package me.concision.warcrimes.docker.swapper;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import me.concision.warcrimes.docker.swapper.api.DockerImage;
import me.concision.warcrimes.docker.swapper.api.DockerImageArchive;
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

    public void execute() {
        try {
            this.index();
            this.parseImages();
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

    @SneakyThrows
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

        DockerImageArchive imageArchive = imageArchives.get(0);
        DockerImageArchive.write(imageArchive, new BufferedOutputStream(new FileOutputStream("test.tar")));

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
