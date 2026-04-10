package com.zeroffa.tdbinpacking;

import com.zeroffa.tdbinpacking.application.BoxType;
import com.zeroffa.tdbinpacking.application.CaseDemand;
import com.zeroffa.tdbinpacking.application.PackingCase;
import com.zeroffa.tdbinpacking.application.PackingRunResult;
import com.zeroffa.tdbinpacking.application.PackingRunner;
import com.zeroffa.tdbinpacking.application.io.BoxTypeCsvReader;
import com.zeroffa.tdbinpacking.application.io.CaseDemandCsvReader;
import com.zeroffa.tdbinpacking.application.io.PackingBatchSummaryWriter;
import com.zeroffa.tdbinpacking.application.io.PackingCaseCsvReader;
import com.zeroffa.tdbinpacking.experiment.RealOrderExperimentRunner;
import com.zeroffa.tdbinpacking.application.io.ItemCsvReader;
import com.zeroffa.tdbinpacking.model.ContainerBox;
import com.zeroffa.tdbinpacking.model.Item;
import com.zeroffa.tdbinpacking.model.PackingResult;
import com.zeroffa.tdbinpacking.solver.ExtremePointBinPacker;
import com.zeroffa.tdbinpacking.visualization.PackingResultVisualizer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class App {

    private static final Path DEFAULT_SINGLE_HTML = Path.of("output", "packing-visualization.html");
    private static final Path DEFAULT_BATCH_OUTPUT_DIR = Path.of("output", "batch-output");
    private static final Path DEFAULT_INSTANCE_RECOMMEND_OUTPUT_DIR = Path.of("output");
    private static final Path DEFAULT_REAL_ORDER_DATA_DIR = Path.of("data");
    private static final Path DEFAULT_REAL_ORDER_EXPERIMENT_DIR = Path.of("output", "real-orders-experiment");
    private static final int DEFAULT_REAL_ORDER_SAMPLE_SIZE = 1000;
    private static final long DEFAULT_REAL_ORDER_SAMPLE_SEED = 93847921L;

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                runSingleDemoCase();
            } else if ("real-orders".equalsIgnoreCase(args[0])) {
                Path dataDirectory = args.length > 1 ? Path.of(args[1]) : DEFAULT_REAL_ORDER_DATA_DIR;
                Path outputDirectory = args.length > 2 ? Path.of(args[2]) : DEFAULT_REAL_ORDER_EXPERIMENT_DIR;
                int sampleSize = args.length > 3 ? Integer.parseInt(args[3]) : DEFAULT_REAL_ORDER_SAMPLE_SIZE;
                long sampleSeed = args.length > 4 ? Long.parseLong(args[4]) : DEFAULT_REAL_ORDER_SAMPLE_SEED;
                runRealOrderExperiment(dataDirectory, outputDirectory, sampleSize, sampleSeed);
            } else if ("box-sweep".equalsIgnoreCase(args[0]) || "recommend".equalsIgnoreCase(args[0])) {
                if (args.length < 4) {
                    printBoxSweepUsage();
                    return;
                }
                Path itemCsvPath = Path.of(args[1]);
                Path boxTypeCsvPath = Path.of(args[2]);
                Path caseCsvPath = Path.of(args[3]);
                Path outputDirectory = args.length > 4 ? Path.of(args[4]) : DEFAULT_INSTANCE_RECOMMEND_OUTPUT_DIR;
                runInstanceRecommendations(itemCsvPath, boxTypeCsvPath, caseCsvPath, outputDirectory);
            } else if ("batch".equalsIgnoreCase(args[0])) {
                if (args.length < 2) {
                    printBatchUsage();
                    return;
                }
                Path inputCsvPath = Path.of(args[1]);
                Path outputDirectory = args.length > 2 ? Path.of(args[2]) : DEFAULT_BATCH_OUTPUT_DIR;
                runBatchCases(inputCsvPath, outputDirectory);
            } else {
                Path inputCsvPath = Path.of(args[0]);
                Path outputDirectory = args.length > 1 ? Path.of(args[1]) : DEFAULT_BATCH_OUTPUT_DIR;
                runBatchCases(inputCsvPath, outputDirectory);
            }
        } catch (IOException exception) {
            System.err.println("I/O failed: " + exception.getMessage());
        }
    }

    private static void runSingleDemoCase() throws IOException {
        PackingCase packingCase = new PackingCase("demo-case-1", new ContainerBox(10, 10, 10), List.of(
                new Item("A", 5, 4, 3),
                new Item("B", 4, 4, 4),
                new Item("C", 6, 3, 2),
                new Item("D", 3, 3, 3),
                new Item("E", 2, 2, 6),
                new Item("F", 2, 5, 2)
        ));

        PackingRunner runner = new PackingRunner(new ExtremePointBinPacker());
        PackingRunResult runResult = runner.run(packingCase);
        printRunResult(runResult);

        PackingResultVisualizer visualizer = new PackingResultVisualizer();
        Path exportedPath = visualizer.exportAsHtml(runResult, DEFAULT_SINGLE_HTML);
        System.out.println();
        System.out.println("3D visualization HTML generated:");
        System.out.println(exportedPath);
    }

    private static void runBatchCases(Path inputCsvPath, Path outputDirectory) throws IOException {
        PackingCaseCsvReader csvReader = new PackingCaseCsvReader();
        List<PackingCase> packingCases = csvReader.read(inputCsvPath);
        if (packingCases.isEmpty()) {
            System.out.println("No valid case found in CSV: " + inputCsvPath.toAbsolutePath());
            return;
        }

        PackingRunner runner = new PackingRunner(new ExtremePointBinPacker());
        PackingResultVisualizer visualizer = new PackingResultVisualizer();
        Path visualizationDir = outputDirectory.resolve("visualizations");
        Path summaryCsvPath = outputDirectory.resolve("batch-summary.csv");

        BatchStats stats = new BatchStats();
        try (PackingBatchSummaryWriter summaryWriter = new PackingBatchSummaryWriter(summaryCsvPath)) {
            runner.runBatch(packingCases, runResult -> {
                try {
                    summaryWriter.append(runResult);
                    Path htmlOutput = visualizationDir.resolve(toSafeFileName(runResult.getPackingCase().getCaseId()) + ".html");
                    visualizer.exportAsHtml(runResult, htmlOutput);
                    stats.accept(runResult);
                    System.out.printf("- case=%s runtime=%.3fms utilization=%.2f%%%n",
                            runResult.getPackingCase().getCaseId(),
                            runResult.getDurationMillis(),
                            runResult.getPackingResult().getUtilization() * 100.0D);
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
        } catch (RuntimeException exception) {
            if (exception.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw exception;
        }

        System.out.printf("Batch completed. Cases: %d%n", packingCases.size());
        System.out.println("Summary CSV: " + summaryCsvPath.toAbsolutePath());
        System.out.println("Visualization Dir: " + visualizationDir.toAbsolutePath());
        System.out.printf("Average runtime: %.3fms%n", stats.averageRuntimeMillis());
        System.out.printf("Average utilization: %.2f%%%n", stats.averageUtilizationPercent());
    }

    private static void runInstanceRecommendations(Path itemCsvPath,
                                                   Path boxTypeCsvPath,
                                                   Path caseCsvPath,
                                                   Path outputDirectory) throws IOException {
        ItemCsvReader itemCsvReader = new ItemCsvReader();
        BoxTypeCsvReader boxTypeCsvReader = new BoxTypeCsvReader();
        CaseDemandCsvReader caseDemandCsvReader = new CaseDemandCsvReader();

        List<Item> itemTemplates = itemCsvReader.read(itemCsvPath);
        List<BoxType> boxTypes = boxTypeCsvReader.read(boxTypeCsvPath);
        List<CaseDemand> caseDemands = caseDemandCsvReader.read(caseCsvPath);
        Map<String, Item> itemCatalog = buildItemCatalog(itemTemplates);

        if (itemTemplates.isEmpty()) {
            System.out.println("No valid items found in CSV: " + itemCsvPath.toAbsolutePath());
            return;
        }
        if (boxTypes.isEmpty()) {
            System.out.println("No valid box types found in CSV: " + boxTypeCsvPath.toAbsolutePath());
            return;
        }
        if (caseDemands.isEmpty()) {
            System.out.println("No valid cases found in CSV: " + caseCsvPath.toAbsolutePath());
            return;
        }

        PackingRunner runner = new PackingRunner(new ExtremePointBinPacker());
        PackingResultVisualizer visualizer = new PackingResultVisualizer();
        Path instancesRootDir = outputDirectory.resolve("instances");
        Path summaryCsvPath = outputDirectory.resolve("summary.csv");

        BatchStats stats = new BatchStats();
        Files.createDirectories(outputDirectory);

        try (BufferedWriter summaryWriter = Files.newBufferedWriter(summaryCsvPath, StandardCharsets.UTF_8)) {
            writeRecommendationSummaryHeader(summaryWriter, boxTypes);
            for (CaseDemand caseDemand : caseDemands) {
                List<Item> caseItems = buildItemsForCaseDemand(caseDemand, itemCatalog);
                String caseId = caseDemand.getCaseId();
                Path caseDir = instancesRootDir.resolve(toSafeFileName(caseId));

                String bestBoxTypeId = null;
                double bestUtilization = -1.0D;
                long bestPackedVolume = -1L;
                long bestDurationNanos = Long.MAX_VALUE;
                Path bestHtmlPath = null;
                Map<String, Double> utilizationByBox = new LinkedHashMap<>();

                for (BoxType boxType : boxTypes) {
                    String boxTypeId = boxType.getBoxTypeId();
                    PackingCase packingCase = new PackingCase(caseId + "::" + boxTypeId, boxType.getContainerBox(), caseItems);
                    PackingRunResult runResult = runner.run(packingCase);
                    stats.accept(runResult);

                    boolean feasible = runResult.getPackingResult().getUnplacedItems().isEmpty();
                    double utilization = feasible ? runResult.getPackingResult().getUtilization() : 0.0D;
                    long packedVolume = runResult.getPackingResult().getPackedVolume();
                    long durationNanos = runResult.getDurationNanos();
                    utilizationByBox.put(boxTypeId, utilization);

                    Path htmlOutput = caseDir.resolve("box-" + toSafeFileName(boxTypeId) + ".html");
                    Path exportedHtml = visualizer.exportAsHtml(runResult, htmlOutput);

                    if (feasible && isBetterRecommendation(utilization, packedVolume, durationNanos,
                            bestUtilization, bestPackedVolume, bestDurationNanos)) {
                        bestBoxTypeId = boxTypeId;
                        bestUtilization = utilization;
                        bestPackedVolume = packedVolume;
                        bestDurationNanos = durationNanos;
                        bestHtmlPath = exportedHtml;
                    }

                    System.out.printf("- instance=%s box=%s runtime=%.3fms utilization=%.2f%%%n",
                            caseId,
                            boxTypeId,
                            runResult.getDurationMillis(),
                            utilization * 100.0D);
                }

                if (bestBoxTypeId == null) {
                    bestBoxTypeId = "NONE";
                    bestUtilization = 0.0D;
                }
                writeRecommendationSummaryRow(summaryWriter, caseId, bestBoxTypeId, bestUtilization, boxTypes, utilizationByBox);
                System.out.printf("  recommended box for %s: %s (%.2f%%)%n", caseId, bestBoxTypeId, bestUtilization * 100.0D);
                if (bestHtmlPath != null) {
                    System.out.println("  recommended html: " + bestHtmlPath.toAbsolutePath());
                }
            }
        }

        System.out.printf("Instance recommendation completed. Instance count: %d%n", caseDemands.size());
        System.out.println("Summary CSV: " + summaryCsvPath.toAbsolutePath());
        System.out.println("Instances Dir: " + instancesRootDir.toAbsolutePath());
        System.out.printf("Average runtime: %.3fms%n", stats.averageRuntimeMillis());
        System.out.printf("Average utilization: %.2f%%%n", stats.averageUtilizationPercent());
    }

    private static void runRealOrderExperiment(Path dataDirectory, Path outputDirectory, int sampleSize, long sampleSeed) throws IOException {
        RealOrderExperimentRunner runner = new RealOrderExperimentRunner(sampleSeed, sampleSize, 10);
        RealOrderExperimentRunner.ExperimentReport report = runner.run(dataDirectory, outputDirectory);
        RealOrderExperimentRunner.ExperimentStats stats = report.stats();

        System.out.println("Real-order experiment completed.");
        System.out.println("Data Dir: " + dataDirectory.toAbsolutePath());
        System.out.printf("Sample seed: %d%n", sampleSeed);
        System.out.printf("Total orders in source: %d%n", stats.totalOrderCount());
        System.out.printf("Sampled orders for experiment: %d%n", stats.sampledOrderCount());
        System.out.printf("Processed sampled orders: %d%n", stats.processedOrders());
        System.out.printf("Cleaned items: %d%n", stats.cleanedItemCount());
        System.out.printf("Cleaned boxes: %d%n", stats.cleanedBoxCount());
        System.out.printf("OK orders: %d%n", stats.okOrders());
        System.out.printf("Missing item orders: %d%n", stats.missingItemOrders());
        System.out.printf("Missing box orders: %d%n", stats.missingBoxOrders());
        System.out.printf("Multiple-box orders: %d%n", stats.multipleBoxOrders());
        System.out.printf("Skipped multi-inner-box orders: %d%n", stats.multipleInnerBoxOrders());
        System.out.printf("No-feasible-box orders: %d%n", stats.noFeasibleBoxOrders());
        System.out.println("Result CSV: " + report.resultCsvPath().toAbsolutePath());
        System.out.println("Sample HTML Dir: " + report.sampleDir().toAbsolutePath());
        System.out.printf("Sample HTML orders: %d%n", report.sampledOrderCount());
        System.out.println("Cleaned Data Dir: " + outputDirectory.resolve("cleaned-data").toAbsolutePath());
    }

    private static void printRunResult(PackingRunResult runResult) {
        PackingResult result = runResult.getPackingResult();
        ContainerBox containerBox = runResult.getPackingCase().getContainerBox();

        System.out.println("=== 3D Bin Packing Result ===");
        System.out.println("Case: " + runResult.getPackingCase().getCaseId());
        System.out.println("Container: " + containerBox);
        System.out.printf("Runtime: %.3f ms%n", runResult.getDurationMillis());
        System.out.printf("Packed Volume / Container Volume: %d / %d%n",
                result.getPackedVolume(), result.getContainerVolume());
        System.out.printf("Utilization: %.2f%%%n", result.getUtilization() * 100.0D);
        System.out.println();
        System.out.println("Placed Items:");
        result.getPlacements().forEach(placement -> System.out.println("- " + placement.toMatrixRow()));

        if (!result.getUnplacedItems().isEmpty()) {
            System.out.println();
            System.out.println("Unplaced Items:");
            result.getUnplacedItems().forEach(item -> System.out.println("- " + item));
        }
    }

    private static String toSafeFileName(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static Map<String, Item> buildItemCatalog(List<Item> itemTemplates) {
        Map<String, Item> itemCatalog = new LinkedHashMap<>();
        for (Item item : itemTemplates) {
            Item existing = itemCatalog.putIfAbsent(item.getId(), item);
            if (existing != null) {
                throw new IllegalArgumentException("Duplicated itemId in items.csv: " + item.getId());
            }
        }
        return itemCatalog;
    }

    private static List<Item> buildItemsForCaseDemand(CaseDemand caseDemand, Map<String, Item> itemCatalog) {
        List<Item> items = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : caseDemand.getItemQuantities().entrySet()) {
            String itemId = entry.getKey();
            int quantity = entry.getValue();
            Item template = itemCatalog.get(itemId);
            if (template == null) {
                throw new IllegalArgumentException("case '" + caseDemand.getCaseId()
                        + "' references unknown itemId: " + itemId);
            }
            for (int index = 1; index <= quantity; index++) {
                items.add(new Item(itemId + "#" + index,
                        template.getSizeX(),
                        template.getSizeY(),
                        template.getSizeZ()));
            }
        }
        return items;
    }

    private static void writeRecommendationSummaryHeader(BufferedWriter summaryWriter, List<BoxType> boxTypes) throws IOException {
        StringBuilder header = new StringBuilder("caseId,recommendedBoxType,recommendedUtilization");
        for (BoxType boxType : boxTypes) {
            header.append(",utilization_").append(toSafeFileName(boxType.getBoxTypeId()));
        }
        summaryWriter.write(header.toString());
        summaryWriter.newLine();
    }

    private static void writeRecommendationSummaryRow(BufferedWriter summaryWriter,
                                                      String caseId,
                                                      String recommendedBoxType,
                                                      double recommendedUtilization,
                                                      List<BoxType> boxTypes,
                                                      Map<String, Double> utilizationByBox) throws IOException {
        StringBuilder row = new StringBuilder();
        row.append(quoteCsv(caseId))
                .append(",")
                .append(quoteCsv(recommendedBoxType))
                .append(",")
                .append(String.format("%.6f", recommendedUtilization));
        for (BoxType boxType : boxTypes) {
            double utilization = utilizationByBox.getOrDefault(boxType.getBoxTypeId(), 0.0D);
            row.append(",").append(String.format("%.6f", utilization));
        }
        summaryWriter.write(row.toString());
        summaryWriter.newLine();
    }

    private static String quoteCsv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static boolean isBetterRecommendation(double candidateUtilization,
                                                  long candidatePackedVolume,
                                                  long candidateDurationNanos,
                                                  double bestUtilization,
                                                  long bestPackedVolume,
                                                  long bestDurationNanos) {
        if (candidateUtilization != bestUtilization) {
            return candidateUtilization > bestUtilization;
        }
        if (candidatePackedVolume != bestPackedVolume) {
            return candidatePackedVolume > bestPackedVolume;
        }
        return candidateDurationNanos < bestDurationNanos;
    }

    private static void printBatchUsage() {
        System.out.println("Usage:");
        System.out.println("  mvn exec:java -Dexec.args=\"batch <cases.csv> [output-dir]\"");
        System.out.println("Or keep old shorthand:");
        System.out.println("  mvn exec:java -Dexec.args=\"<cases.csv> [output-dir]\"");
    }

    private static void printBoxSweepUsage() {
        System.out.println("Usage:");
        System.out.println("  mvn exec:java -Dexec.args=\"box-sweep <items.csv> <box-types.csv> <cases.csv> [output-dir]\"");
    }

    private static class BatchStats {
        private long caseCount;
        private double totalRuntimeMillis;
        private double totalUtilization;

        private void accept(PackingRunResult runResult) {
            caseCount++;
            totalRuntimeMillis += runResult.getDurationMillis();
            totalUtilization += runResult.getPackingResult().getUtilization();
        }

        private double averageRuntimeMillis() {
            return caseCount == 0 ? 0.0D : totalRuntimeMillis / caseCount;
        }

        private double averageUtilizationPercent() {
            return caseCount == 0 ? 0.0D : (totalUtilization / caseCount) * 100.0D;
        }
    }

}
