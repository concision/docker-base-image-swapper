package me.concision.warcrimes.docker.swapper;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CommandArguments {
    @NonNull
    public final Namespace namespace;

    // base images
    public final String oldBaseImageName;
    public final String newBaseImageName;

    // application images
    public final String inputImageName;
    public final File outputImageFile;

    // Docker image archives
    public final List<File> archiveFiles;

    public static CommandArguments from(@NonNull Namespace namespace) {
        return new CommandArguments(
                namespace,
                // base images
                namespace.getString("base_image_old"),
                namespace.getString("base_image_new"),
                // input/output
                namespace.get("image_input"),
                namespace.get("image_output"),
                // images
                namespace.getList("images")
        );
    }
}
