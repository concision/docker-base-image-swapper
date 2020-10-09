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
    public final String outputImageName;
    public final File outputImageFile;

    // Docker image archives
    public final List<File> archiveFiles;

    public static CommandArguments from(@NonNull Namespace namespace) {
        return new CommandArguments(
                namespace,
                // base images
                namespace.getString("base_image_old_tag"),
                namespace.getString("base_image_new_tag"),
                // input/output
                namespace.getString("image_input_tag"),
                namespace.getString("image_output_tag"),
                namespace.get("image_output"),
                // images
                namespace.getList("images")
        );
    }
}
