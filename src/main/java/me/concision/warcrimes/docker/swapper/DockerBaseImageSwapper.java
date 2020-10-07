package me.concision.warcrimes.docker.swapper;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.concision.warcrimes.docker.swapper.util.RandomAccessTarArchiveFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
            archives.add(new RandomAccessTarArchiveFile(archiveFile));
        }
        // index archives
        for (RandomAccessTarArchiveFile archive : archives) {
            archive.createIndex();
        }
    }
}
