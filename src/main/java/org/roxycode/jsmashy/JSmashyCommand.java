package org.roxycode.jsmashy;

import io.micronaut.configuration.picocli.PicocliRunner;
import jakarta.inject.Inject;
import org.roxycode.jsmashy.service.FileProcessingService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Command(name = "jsmashy", description = "Smash your codebase into an XML for LLMs",
        mixinStandardHelpOptions = true)
public class JSmashyCommand implements Runnable {

    @Option(names = {"-i", "--input"}, description = "Input directory", defaultValue = ".")
    Path input;

    @Option(names = {"-o", "--output"}, description = "Output XML file")
    Path output;

    @Option(names = {"--raw"}, description = "Do not perform semantic compression")
    boolean raw;

    @Option(names = {"--exclude"}, description = "Additional patterns to exclude")
    List<String> userExcludes = new ArrayList<>();

    @Inject
    FileProcessingService fileProcessingService;

    public static void main(String[] args) {
        PicocliRunner.run(JSmashyCommand.class, args);
    }

    @Override
    public void run() {
        List<String> excludes = new ArrayList<>(List.of(".git", "target", "node_modules", ".idea", ".vscode"));
        if (userExcludes != null) {
            excludes.addAll(userExcludes);
        }

        System.out.println("Processing " + input.toAbsolutePath() + "...");
        String result = fileProcessingService.processDirectory(input, excludes, raw);

        if (output != null) {
            try {
                Files.writeString(output, result);
                System.out.println("Result written to " + output);
            } catch (Exception e) {
                System.err.println("Failed to write output: " + e.getMessage());
            }
        } else {
            System.out.println(result);
        }
    }
}
