package com.zeroffa.tdbinpacking.application.io;

import com.zeroffa.tdbinpacking.application.PackingRunResult;
import com.zeroffa.tdbinpacking.model.PackingResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class PackingBatchSummaryWriter implements AutoCloseable {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final BufferedWriter writer;

    public PackingBatchSummaryWriter(Path outputPath) throws IOException {
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        this.writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8);
        writer.write("caseId,startedAt,runtimeMs,packedVolume,containerVolume,utilization,placedCount,unplacedCount");
        writer.newLine();
    }

    public void append(PackingRunResult runResult) throws IOException {
        PackingResult result = runResult.getPackingResult();
        String row = String.format(
                "%s,%s,%.3f,%d,%d,%.6f,%d,%d",
                sanitize(runResult.getPackingCase().getCaseId()),
                TIME_FORMATTER.format(runResult.getStartedAt()),
                runResult.getDurationMillis(),
                result.getPackedVolume(),
                result.getContainerVolume(),
                result.getUtilization(),
                result.getPlacements().size(),
                result.getUnplacedItems().size()
        );
        writer.write(row);
        writer.newLine();
    }

    private String sanitize(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    @Override
    public void close() throws IOException {
        writer.flush();
        writer.close();
    }
}
