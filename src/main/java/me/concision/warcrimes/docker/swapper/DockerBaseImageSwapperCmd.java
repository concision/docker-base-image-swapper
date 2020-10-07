package me.concision.warcrimes.docker.swapper;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

public class DockerBaseImageSwapperCmd {
    public static void main(String[] cliArgs) {
        // default arguments to --help flag
        if (cliArgs == null || cliArgs.length == 0) {
            cliArgs = new String[]{"--help"};
        }

        // construct argument parser
        ArgumentParser parser = ArgumentParsers.newFor("unpacker")
                // flag prefix
                .prefixChars("--")
                // width
                .defaultFormatWidth(128)
                .terminalWidthDetection(true)
                // build parser
                .build()
                .usage("java -jar docker-base-image-swapper.jar")
                .description("A CLI tool to swap out the base image of an already-existing built Docker image.");

        parser.addArgument("--old-base-image")
                .help("The .tar archive of the old base image that will be replaced in the application image")
                .metavar("old-base-image.tar")
                .dest("base_image_old")
                .required(true)
                .type(Arguments.fileType().verifyCanRead());

        parser.addArgument("--new-base-image")
                .help("The .tar archive of the new base image that the application image will be swapped to")
                .metavar("new-base-image.tar")
                .dest("base_image_new")
                .required(true)
                .type(Arguments.fileType().verifyCanRead());

        parser.addArgument("--application-image")
                .help("The .tar archive of the application image that needs to the base image to be swapped")
                .metavar("app-image.tar")
                .dest("app_image_input")
                .required(true)
                .type(Arguments.fileType().verifyCanRead());

        parser.addArgument("--output-image")
                .help("The path location to write the new .tar archived of the newly created application image")
                .metavar("swapped-base-app-image.tar")
                .dest("app_image_output")
                .required(true)
                .type(Arguments.fileType().verifyCanCreate());

        // parse namespace, or exit runtime
        Namespace namespace = parser.parseArgsOrFail(cliArgs);


        // convert to runtime configuration
        CommandArguments arguments = CommandArguments.from(namespace);

        // execute extraction process
        System.out.println("Argument namespace: " + namespace);
        try {
            new DockerBaseImageSwapper(arguments).execute();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            System.exit(-1);
        }
    }
}
