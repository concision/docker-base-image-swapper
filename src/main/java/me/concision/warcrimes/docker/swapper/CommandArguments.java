package me.concision.warcrimes.docker.swapper;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CommandArguments {
    @NonNull
    public final Namespace namespace;

    // base images
    public final File oldBaseImage;
    public final File newBaseImage;

    // application images
    public final File inputImage;
    public final File outputImage;

    public static CommandArguments from(@NonNull Namespace namespace) {

        return new CommandArguments(
                namespace,
                // base images
                namespace.get("base_image_old"),
                namespace.get("base_image_new"),
                // source
                namespace.get("app_image_input"),
                namespace.get("app_image_output")
        );
    }
}
