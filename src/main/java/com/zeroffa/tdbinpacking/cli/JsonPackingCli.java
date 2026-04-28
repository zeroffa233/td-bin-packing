package com.zeroffa.tdbinpacking.cli;

import com.zeroffa.tdbinpacking.api.PackingApi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonPackingCli {

    public static void main(String[] args) throws IOException {
        CliOptions options = CliOptions.parse(args);
        String jsonInput = readInput(options);
        String jsonOutput = new PackingApi().packJson(jsonInput);
        writeOutput(options, jsonOutput);
        if (isFailureResponse(jsonOutput)) {
            System.exit(1);
        }
    }

    private static String readInput(CliOptions options) throws IOException {
        if (options.jsonText != null) {
            return options.jsonText;
        }
        if (options.inputPath != null) {
            byte[] bytes = Files.readAllBytes(options.inputPath);
            return new String(bytes, StandardCharsets.UTF_8);
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = System.in.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void writeOutput(CliOptions options, String jsonOutput) throws IOException {
        if (options.outputPath == null) {
            System.out.println(jsonOutput);
            return;
        }
        Path parent = options.outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(options.outputPath, jsonOutput.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isFailureResponse(String jsonOutput) {
        return !jsonOutput.contains("\"code\": 0");
    }

    private static final class CliOptions {
        private final Path inputPath;
        private final Path outputPath;
        private final String jsonText;

        private CliOptions(Path inputPath, Path outputPath, String jsonText) {
            this.inputPath = inputPath;
            this.outputPath = outputPath;
            this.jsonText = jsonText;
        }

        private static CliOptions parse(String[] args) {
            Path inputPath = null;
            Path outputPath = null;
            String jsonText = null;
            boolean readStdin = args.length == 0;

            for (int index = 0; index < args.length; index++) {
                String arg = args[index];
                if ("--input".equals(arg) || "-i".equals(arg)) {
                    inputPath = Paths.get(requiredValue(args, ++index, arg));
                } else if ("--output".equals(arg) || "-o".equals(arg)) {
                    outputPath = Paths.get(requiredValue(args, ++index, arg));
                } else if ("--json".equals(arg)) {
                    jsonText = requiredValue(args, ++index, arg);
                } else if ("--stdin".equals(arg)) {
                    readStdin = true;
                } else if (inputPath == null) {
                    inputPath = Paths.get(arg);
                } else if (outputPath == null) {
                    outputPath = Paths.get(arg);
                } else {
                    throw new IllegalArgumentException("unknown argument: " + arg);
                }
            }

            if (jsonText != null && inputPath != null) {
                throw new IllegalArgumentException("--json cannot be combined with --input");
            }
            if (readStdin && inputPath == null && jsonText == null) {
                return new CliOptions(null, outputPath, null);
            }
            return new CliOptions(inputPath, outputPath, jsonText);
        }

        private static String requiredValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException(option + " requires a value");
            }
            return args[index];
        }
    }
}
