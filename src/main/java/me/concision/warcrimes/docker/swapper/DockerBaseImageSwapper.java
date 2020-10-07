package me.concision.warcrimes.docker.swapper;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import me.concision.warcrimes.docker.swapper.util.RandomAccessTarArchiveFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Log4j2
@RequiredArgsConstructor
public class DockerBaseImageSwapper {
    @Getter
    @NonNull
    private final CommandArguments args;

    @SneakyThrows
    public void execute() {
        List<RandomAccessTarArchiveFile> archives = new ArrayList<>();

        // check file exists and subsequently acquire file locks
        for (File archiveFile : args.archiveFiles) {
            log.info("Acquiring image file lock: {}", archiveFile);
            archives.add(new RandomAccessTarArchiveFile(archiveFile));
        }
        // index archives
        for (RandomAccessTarArchiveFile archive : archives) {
            log.info("Reading and indexing .tar archive: {}", archive.getFile());
            archive.createIndex();
        }
    }
}
