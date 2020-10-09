package me.concision.warcrimes.docker.swapper;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.impl.type.FileArgumentType;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class DockerBaseImageSwapperCmd {
    public static void main(String[] cliArgs) {
        // default arguments to --help flag
        if (cliArgs == null || cliArgs.length == 0) {
            cliArgs = new String[]{"--help"};
        }

        // construct argument parser
        ArgumentParser parser = ArgumentParsers.newFor("swapper")
                // flag prefix
                .prefixChars("--")
                // width
                .defaultFormatWidth(128)
                .terminalWidthDetection(true)
                // build parser
                .build()
                .usage("java -jar docker-base-image-swapper.jar")
                .description("A CLI tool to swap out the base image of an already-existing built Docker image.");

        parser.addArgument("--old-base-tag")
                .help("The tag of the old base image that will be replaced in the application image")
                .metavar("image:tag")
                .dest("base_image_old_tag")
                .required(true)
                .type(String.class);

        parser.addArgument("--new-base-tag")
                .help("The tag of the new base image that the application image will be swapped to")
                .metavar("image:tag")
                .dest("base_image_new_tag")
                .required(true)
                .type(String.class);

        parser.addArgument("--input-tag")
                .help("The tag of the application image that needs the base image swapped")
                .metavar("image:tag")
                .dest("image_input_tag")
                .required(true)
                .type(String.class);

        parser.addArgument("--output-tag")
                .help("The output tag of the new built application image")
                .metavar("image:tag")
                .dest("image_output_tag")
                .required(true)
                .type(String.class);

        parser.addArgument("--output-image")
                .help("The file path to write the new .tar archived of the new built application image.\n" +
                        "If unspecified, writes to standard out.")
                .metavar("swapped-base-app-image.tar")
                .dest("image_output")
                .required(true)
                .type(Arguments.fileType().verifyCanCreate());

        parser.addArgument("images")
                .help("List of Docker image archives to search for tagged images")
                .metavar("image.tar")
                .dest("images")
                .required(true)
                .nargs("*")
                .type(new FileArgumentType().verifyCanRead());


        // parse namespace, or exit runtime
        Namespace namespace = parser.parseArgsOrFail(cliArgs);

        // convert to runtime configuration
        CommandArguments arguments = CommandArguments.from(namespace);

        // initialize logging mechanism
        Logger log = LogManager.getLogger(DockerBaseImageSwapperCmd.class);
        log.debug("Namespace: {}", namespace);

        // execute extraction process
        try {
            new DockerBaseImageSwapper(arguments)
                    .execute();
        } catch (Throwable throwable) {
            log.info("Failed to execute Docker image swapping process", throwable);
            System.exit(-1);
        }
    }
}
