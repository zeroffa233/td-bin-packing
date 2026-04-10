package com.zeroffa.tdbinpacking.experiment;

import com.zeroffa.tdbinpacking.application.PackingCase;
import com.zeroffa.tdbinpacking.application.PackingPreparation;
import com.zeroffa.tdbinpacking.application.PackingRunResult;
import com.zeroffa.tdbinpacking.application.PackingRunner;
import com.zeroffa.tdbinpacking.model.ContainerBox;
import com.zeroffa.tdbinpacking.model.Item;
import com.zeroffa.tdbinpacking.model.ItemAttribute;
import com.zeroffa.tdbinpacking.solver.ExtremePointBinPacker;
import com.zeroffa.tdbinpacking.visualization.PackingResultVisualizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class RealOrderExperimentRunner {

    private static final Charset RAW_ORDER_CHARSET = Charset.forName("GB18030");
    private static final BigDecimal ITEM_SCALE_FACTOR = new BigDecimal("10000");
    private static final BigDecimal BOX_INTEGER_SCALE_FACTOR = new BigDecimal("100");
    private static final BigDecimal METER_TO_CENTIMETER = new BigDecimal("100");
    private static final int UTILIZATION_SCALE = 10;
    private static final long DEFAULT_SAMPLE_SEED = 93847921L;
    private static final int DEFAULT_SAMPLE_HTML_SIZE = 10;
    private static final int DEFAULT_EXPERIMENT_ORDER_SAMPLE_SIZE = 1000;
    private static final int PROGRESS_INTERVAL = 2000;

    private final PackingRunner packingRunner;
    private final PackingResultVisualizer visualizer;
    private final long sampleSeed;
    private final int sampleHtmlSize;
    private final int experimentOrderSampleSize;

    public RealOrderExperimentRunner() {
        this(DEFAULT_SAMPLE_SEED, DEFAULT_EXPERIMENT_ORDER_SAMPLE_SIZE, DEFAULT_SAMPLE_HTML_SIZE);
    }

    public RealOrderExperimentRunner(long sampleSeed, int experimentOrderSampleSize, int sampleHtmlSize) {
        this.packingRunner = new PackingRunner(new ExtremePointBinPacker());
        this.visualizer = new PackingResultVisualizer();
        this.sampleSeed = sampleSeed;
        this.sampleHtmlSize = sampleHtmlSize;
        this.experimentOrderSampleSize = experimentOrderSampleSize;
    }

    public ExperimentReport run(Path dataDirectory, Path outputDirectory) throws IOException {
        Path itemPath = dataDirectory.resolve("item.csv");
        Path boxPath = dataDirectory.resolve("box.csv");
        Path orderPath = dataDirectory.resolve("orders.csv");
        Path rawOrderPath = dataDirectory.resolve("raw_orders.csv");

        Catalog catalog = loadCatalog(itemPath, boxPath, rawOrderPath);
        Files.createDirectories(outputDirectory);
        exportCleanedCatalog(catalog, outputDirectory.resolve("cleaned-data"));

        Path resultCsvPath = outputDirectory.resolve("order-utilization-comparison.csv");
        SampledOrders sampledOrders = sampleOrderBlocks(orderPath, experimentOrderSampleSize);
        ReservoirSampler<SampleOrder> sampler = new ReservoirSampler<>(sampleHtmlSize, new Random(sampleSeed + 1));
        Map<String, AlgorithmDecision> algorithmDecisionCache = new HashMap<>();
        ExperimentStats stats = new ExperimentStats(
                catalog.cleanedItemCount(),
                catalog.cleanedBoxCount(),
                sampledOrders.totalOrderCount(),
                sampledOrders.orderBlocks().size()
        );

        try (BufferedWriter writer = Files.newBufferedWriter(resultCsvPath, StandardCharsets.UTF_8)) {
            writer.write("orderId,actualBoxId,actualUtilization,algorithmBoxId,algorithmUtilization,status");
            writer.newLine();
            for (OrderBlock orderBlock : sampledOrders.orderBlocks()) {
                processOrderBlock(orderBlock, catalog, algorithmDecisionCache, writer, sampler, outputDirectory, stats);
            }
        }

        Path sampleDir = outputDirectory.resolve("sample-orders");
        recreateDirectory(sampleDir);
        exportSampleHtmls(sampler.getSamples(), catalog, sampleDir);
        return new ExperimentReport(resultCsvPath, sampleDir, stats, sampler.getSamples().size());
    }

    private void exportCleanedCatalog(Catalog catalog, Path cleanedDataDir) throws IOException {
        Files.createDirectories(cleanedDataDir);

        Path cleanedItemPath = cleanedDataDir.resolve("item.cleaned.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(cleanedItemPath, StandardCharsets.UTF_8)) {
            writer.write("item-id,length,width,height,attribute");
            writer.newLine();
            for (Map.Entry<String, ItemEntry> entry : catalog.itemCatalog().entrySet()) {
                Item item = entry.getValue().item();
                writer.write(quoteCsv(entry.getKey()) + ","
                        + item.getSizeX() + ","
                        + item.getSizeY() + ","
                        + item.getSizeZ() + ","
                        + quoteCsv(item.getAttribute().getCsvValue()));
                writer.newLine();
            }
        }

        Path cleanedBoxPath = cleanedDataDir.resolve("box.cleaned.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(cleanedBoxPath, StandardCharsets.UTF_8)) {
            writer.write("box-id,length,width,height");
            writer.newLine();
            for (Map.Entry<String, BoxEntry> entry : catalog.boxCatalog().entrySet()) {
                ContainerBox box = entry.getValue().containerBox();
                writer.write(quoteCsv(entry.getKey()) + ","
                        + box.getSizeX() + ","
                        + box.getSizeY() + ","
                        + box.getSizeZ());
                writer.newLine();
            }
        }
    }

    private SampledOrders sampleOrderBlocks(Path orderPath, int targetSampleSize) throws IOException {
        ReservoirSampler<OrderBlock> sampler = new ReservoirSampler<>(targetSampleSize, new Random(sampleSeed));
        long totalOrderCount = 0;

        try (BufferedReader reader = Files.newBufferedReader(orderPath, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            OrderBlockBuilder currentBlock = null;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 || line.isBlank()) {
                    continue;
                }
                List<String> columns = splitCsvLine(line);
                if (columns.size() != 4) {
                    throw new IllegalArgumentException("Invalid orders.csv column size at line " + lineNumber + ": expected 4");
                }

                String orderId = columns.get(0).trim();
                String itemId = columns.get(1).trim();
                int quantity = parsePositiveInt(columns.get(2), "quantity", lineNumber);
                String boxId = columns.get(3).trim();

                if (currentBlock == null || currentBlock.isSameOrder(orderId)) {
                    if (currentBlock == null) {
                        currentBlock = new OrderBlockBuilder(orderId);
                    }
                } else {
                    sampler.consider(currentBlock.build(), totalOrderCount);
                    totalOrderCount++;
                    currentBlock = new OrderBlockBuilder(orderId);
                }
                currentBlock.addLine(itemId, quantity, boxId);
            }

            if (currentBlock != null) {
                sampler.consider(currentBlock.build(), totalOrderCount);
                totalOrderCount++;
            }
        }

        return new SampledOrders(sampler.getSamples(), totalOrderCount);
    }

    private Catalog loadCatalog(Path itemPath, Path boxPath, Path rawOrderPath) throws IOException {
        Map<String, ItemEntry> itemCatalog = new LinkedHashMap<>();
        Map<String, BoxEntry> boxCatalog = new LinkedHashMap<>();
        Map<String, String> itemAttributes = loadItemAttributes(rawOrderPath);

        int cleanedItemCount = 0;
        try (BufferedReader reader = Files.newBufferedReader(itemPath, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 || line.isBlank()) {
                    continue;
                }
                List<String> columns = splitCsvLine(line);
                if (columns.size() != 4) {
                    throw new IllegalArgumentException("Invalid item.csv column size at line " + lineNumber + ": expected 4");
                }
                String itemId = columns.get(0).trim();
                BigDecimal length = parseDimension(columns.get(1), "item length", lineNumber);
                BigDecimal width = parseDimension(columns.get(2), "item width", lineNumber);
                BigDecimal height = parseDimension(columns.get(3), "item height", lineNumber);
                if (isZeroDimension(length, width, height)) {
                    continue;
                }
                int scaledLength = scaleItemDimension(length);
                int scaledWidth = scaleItemDimension(width);
                int scaledHeight = scaleItemDimension(height);
                if (scaledLength <= 0 || scaledWidth <= 0 || scaledHeight <= 0) {
                    continue;
                }

                Item item = new Item(
                        itemId,
                        scaledLength,
                        scaledWidth,
                        scaledHeight,
                        ItemAttribute.fromCsvValue(itemAttributes.getOrDefault(itemId, "product"))
                );
                itemCatalog.put(itemId, new ItemEntry(itemId, item));
                cleanedItemCount++;
            }
        }

        int cleanedBoxCount = 0;
        try (BufferedReader reader = Files.newBufferedReader(boxPath, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 || line.isBlank()) {
                    continue;
                }
                List<String> columns = splitCsvLine(line);
                if (columns.size() != 4) {
                    throw new IllegalArgumentException("Invalid box.csv column size at line " + lineNumber + ": expected 4");
                }
                String boxId = columns.get(0).trim();
                BigDecimal length = parseDimension(columns.get(1), "box length", lineNumber);
                BigDecimal width = parseDimension(columns.get(2), "box width", lineNumber);
                BigDecimal height = parseDimension(columns.get(3), "box height", lineNumber);
                if (isZeroDimension(length, width, height)) {
                    continue;
                }
                BigDecimal normalizedLength = normalizeBoxDimensionToCentimeter(length, width);
                BigDecimal normalizedWidth = normalizeBoxDimensionToCentimeter(width, length);
                BigDecimal normalizedHeight = normalizeBoxHeightToCentimeter(height, length, width);

                int scaledLength = scaleBoxDimension(normalizedLength);
                int scaledWidth = scaleBoxDimension(normalizedWidth);
                int scaledHeight = scaleBoxDimension(normalizedHeight);
                if (scaledLength <= 0 || scaledWidth <= 0 || scaledHeight <= 0) {
                    continue;
                }

                ContainerBox box = new ContainerBox(
                        scaledLength,
                        scaledWidth,
                        scaledHeight
                );
                boxCatalog.put(normalizeBoxId(boxId), new BoxEntry(boxId, box));
                cleanedBoxCount++;
            }
        }

        List<BoxEntry> sortedBoxes = new ArrayList<>(boxCatalog.values());
        sortedBoxes.sort(Comparator
                .comparing((BoxEntry entry) -> entry.volume())
                .thenComparing(BoxEntry::boxId));
        return new Catalog(itemCatalog, boxCatalog, Collections.unmodifiableList(sortedBoxes), cleanedItemCount, cleanedBoxCount);
    }

    private Map<String, String> loadItemAttributes(Path rawOrderPath) throws IOException {
        Map<String, String> attributes = new HashMap<>();
        if (!Files.exists(rawOrderPath)) {
            return attributes;
        }

        try (BufferedReader reader = Files.newBufferedReader(rawOrderPath, RAW_ORDER_CHARSET)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 || line.isBlank()) {
                    continue;
                }
                List<String> columns = splitCsvLine(line);
                if (columns.size() != 5) {
                    throw new IllegalArgumentException("Invalid raw_orders.csv column size at line " + lineNumber + ": expected 5");
                }
                String itemId = columns.get(1).trim();
                String itemName = columns.get(2).trim();
                String attribute = classifyAttribute(itemName);
                attributes.merge(itemId, attribute, this::pickHigherPriorityAttribute);
            }
        }
        return attributes;
    }

    private String classifyAttribute(String itemName) {
        if (itemName.contains("袋")) {
            return "bag";
        }
        if (itemName.contains("礼盒")) {
            return "box";
        }
        return "product";
    }

    private String pickHigherPriorityAttribute(String current, String candidate) {
        if (attributePriority(candidate) > attributePriority(current)) {
            return candidate;
        }
        return current;
    }

    private int attributePriority(String attribute) {
        return switch (attribute) {
            case "bag" -> 3;
            case "box" -> 2;
            default -> 1;
        };
    }

    private void processOrderBlock(OrderBlock orderBlock,
                                   Catalog catalog,
                                   Map<String, AlgorithmDecision> algorithmDecisionCache,
                                   BufferedWriter writer,
                                   ReservoirSampler<SampleOrder> sampler,
                                   Path outputDirectory,
                                   ExperimentStats stats) throws IOException {
        stats.processedOrders++;
        if (stats.processedOrders % PROGRESS_INTERVAL == 0) {
            System.out.printf("Processed %d sampled orders...%n", stats.processedOrders);
        }

        EvaluationResult evaluation = evaluateOrderBlock(orderBlock, catalog, algorithmDecisionCache);
        stats.accept(evaluation.status());
        if (evaluation.includeInComparison()) {
            writer.write(evaluation.toCsvRow());
            writer.newLine();
        }

        if (evaluation.sampleOrder() != null) {
            sampler.consider(evaluation.sampleOrder(), stats.validOrdersForSampling);
            stats.validOrdersForSampling++;
        }
    }

    private EvaluationResult evaluateOrderBlock(OrderBlock orderBlock,
                                                Catalog catalog,
                                                Map<String, AlgorithmDecision> algorithmDecisionCache) {
        if (!orderBlock.hasSingleActualBox()) {
            return EvaluationResult.invalid(orderBlock.orderId(), orderBlock.firstBoxId(), "MULTIPLE_BOX_IDS");
        }

        BoxEntry actualBoxEntry = catalog.boxCatalog().get(normalizeBoxId(orderBlock.firstBoxId()));
        if (actualBoxEntry == null) {
            return EvaluationResult.invalid(orderBlock.orderId(), orderBlock.firstBoxId(), "ACTUAL_BOX_MISSING");
        }

        List<Item> expandedItems = new ArrayList<>();
        Map<String, Integer> normalizedQuantities = new LinkedHashMap<>();

        for (Map.Entry<String, Integer> entry : orderBlock.itemQuantities().entrySet()) {
            ItemEntry itemEntry = catalog.itemCatalog().get(entry.getKey());
            if (itemEntry == null) {
                return EvaluationResult.invalid(orderBlock.orderId(), orderBlock.firstBoxId(), "ITEM_MISSING");
            }
            Item template = itemEntry.item();
            int quantity = entry.getValue();
            normalizedQuantities.put(entry.getKey(), quantity);
            for (int index = 1; index <= quantity; index++) {
                expandedItems.add(new Item(
                        entry.getKey() + "#" + index,
                        template.getSizeX(),
                        template.getSizeY(),
                        template.getSizeZ(),
                        template.getAttribute()
                ));
            }
        }

        PackingPreparation preparation;
        try {
            preparation = packingRunner.prepareItems(expandedItems);
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("Multiple box items found")) {
                return EvaluationResult.skipped(orderBlock.orderId(), orderBlock.firstBoxId(), "MULTIPLE_INNER_BOX_ITEMS");
            }
            throw new IllegalArgumentException("Order '" + orderBlock.orderId() + "' failed preprocessing: "
                    + exception.getMessage(), exception);
        }
        BigDecimal actualUtilization = divide(BigInteger.valueOf(preparation.getPreparedVolume()), actualBoxEntry.volume());
        String signature = buildSignature(normalizedQuantities);
        AlgorithmDecision decision = algorithmDecisionCache.get(signature);
        if (decision == null) {
            decision = findBestAlgorithmDecision(expandedItems, preparation, catalog.sortedBoxes());
            algorithmDecisionCache.put(signature, decision);
        }

        SampleOrder sampleOrder = null;
        if ("OK".equals(decision.status())) {
            sampleOrder = new SampleOrder(orderBlock.orderId(), actualBoxEntry.boxId(), decision.boxId(), expandedItems);
        }

        return new EvaluationResult(
                orderBlock.orderId(),
                actualBoxEntry.boxId(),
                actualUtilization,
                decision.boxId(),
                decision.utilization(),
                decision.status(),
                sampleOrder,
                true
        );
    }

    private AlgorithmDecision findBestAlgorithmDecision(List<Item> originalItems,
                                                        PackingPreparation preparation,
                                                        List<BoxEntry> sortedBoxes) {
        List<Item> preparedItems = preparation.getPreparedItems();
        BigInteger totalItemVolume = BigInteger.valueOf(preparation.getPreparedVolume());
        for (BoxEntry boxEntry : sortedBoxes) {
            if (boxEntry.volume().compareTo(totalItemVolume) < 0) {
                continue;
            }
            if (!allItemsCanIndividuallyFit(preparedItems, boxEntry.containerBox())) {
                continue;
            }

            PackingCase packingCase = new PackingCase("selection::" + boxEntry.boxId(), boxEntry.containerBox(), originalItems);
            PackingRunResult runResult = packingRunner.runPrepared(packingCase, preparation);
            if (runResult.getPackingResult().isAllPlaced()) {
                return new AlgorithmDecision(
                        boxEntry.boxId(),
                        divide(totalItemVolume, boxEntry.volume()),
                        "OK"
                );
            }
        }
        return new AlgorithmDecision("NONE", BigDecimal.ZERO.setScale(UTILIZATION_SCALE, RoundingMode.HALF_UP), "NO_FEASIBLE_BOX");
    }

    private boolean allItemsCanIndividuallyFit(List<Item> items, ContainerBox containerBox) {
        int[] boxDims = sortedDims(containerBox.getSizeX(), containerBox.getSizeY(), containerBox.getSizeZ());
        for (Item item : items) {
            int[] itemDims = sortedDims(item.getSizeX(), item.getSizeY(), item.getSizeZ());
            if (itemDims[0] > boxDims[0] || itemDims[1] > boxDims[1] || itemDims[2] > boxDims[2]) {
                return false;
            }
        }
        return true;
    }

    private int[] sortedDims(int first, int second, int third) {
        int[] dims = new int[]{first, second, third};
        java.util.Arrays.sort(dims);
        return dims;
    }

    private void exportSampleHtmls(List<SampleOrder> sampleOrders, Catalog catalog, Path sampleDir) throws IOException {
        Files.createDirectories(sampleDir);
        for (SampleOrder sampleOrder : sampleOrders) {
            BoxEntry actualBox = catalog.boxCatalog().get(normalizeBoxId(sampleOrder.actualBoxId()));
            BoxEntry algorithmBox = catalog.boxCatalog().get(normalizeBoxId(sampleOrder.algorithmBoxId()));
            if (actualBox == null || algorithmBox == null) {
                continue;
            }

            Path orderDir = sampleDir.resolve(sanitizeFileName(sampleOrder.orderId()));
            recreateDirectory(orderDir);

            PackingRunResult actualRun = packingRunner.run(new PackingCase(
                    sampleOrder.orderId() + "::actual::" + sampleOrder.actualBoxId(),
                    actualBox.containerBox(),
                    sampleOrder.items()
            ));
            visualizer.exportAsHtml(actualRun, orderDir.resolve("actual-box-" + sanitizeFileName(sampleOrder.actualBoxId()) + ".html"));

            PackingRunResult algorithmRun = packingRunner.run(new PackingCase(
                    sampleOrder.orderId() + "::algorithm::" + sampleOrder.algorithmBoxId(),
                    algorithmBox.containerBox(),
                    sampleOrder.items()
            ));
            visualizer.exportAsHtml(algorithmRun, orderDir.resolve("algorithm-box-" + sanitizeFileName(sampleOrder.algorithmBoxId()) + ".html"));
        }
    }

    private String buildSignature(Map<String, Integer> itemQuantities) {
        StringBuilder builder = new StringBuilder();
        itemQuantities.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> builder.append(entry.getKey()).append(":").append(entry.getValue()).append("|"));
        return builder.toString();
    }

    private BigInteger volumeOf(Item item) {
        return BigInteger.valueOf(item.getSizeX())
                .multiply(BigInteger.valueOf(item.getSizeY()))
                .multiply(BigInteger.valueOf(item.getSizeZ()));
    }

    private BigInteger volumeOf(ContainerBox box) {
        return BigInteger.valueOf(box.getSizeX())
                .multiply(BigInteger.valueOf(box.getSizeY()))
                .multiply(BigInteger.valueOf(box.getSizeZ()));
    }

    private BigDecimal divide(BigInteger numerator, BigInteger denominator) {
        return new BigDecimal(numerator).divide(new BigDecimal(denominator), UTILIZATION_SCALE, RoundingMode.HALF_UP);
    }

    private boolean isZeroDimension(BigDecimal length, BigDecimal width, BigDecimal height) {
        return length.signum() == 0 || width.signum() == 0 || height.signum() == 0;
    }

    private BigDecimal parseDimension(String rawValue, String fieldName, int lineNumber) {
        try {
            return new BigDecimal(rawValue.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid number for " + fieldName + " at line " + lineNumber);
        }
    }

    private int scaleItemDimension(BigDecimal value) {
        return value.multiply(ITEM_SCALE_FACTOR).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private int scaleBoxDimension(BigDecimal value) {
        return value.multiply(BOX_INTEGER_SCALE_FACTOR).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private BigDecimal normalizeBoxDimensionToCentimeter(BigDecimal dimension, BigDecimal pairedDimension) {
        if (dimension.compareTo(BigDecimal.ONE) < 0 || pairedDimension.compareTo(BigDecimal.ONE) < 0) {
            return dimension.multiply(METER_TO_CENTIMETER);
        }
        return dimension;
    }

    private BigDecimal normalizeBoxHeightToCentimeter(BigDecimal height, BigDecimal length, BigDecimal width) {
        if (length.compareTo(BigDecimal.ONE) < 0 || width.compareTo(BigDecimal.ONE) < 0) {
            return height.multiply(METER_TO_CENTIMETER);
        }
        return height;
    }

    private int parsePositiveInt(String rawValue, String fieldName, int lineNumber) {
        try {
            int value = Integer.parseInt(rawValue.trim());
            if (value <= 0) {
                throw new IllegalArgumentException(fieldName + " must be positive at line " + lineNumber);
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid number for " + fieldName + " at line " + lineNumber);
        }
    }

    private List<String> splitCsvLine(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder(line.length());
        boolean inQuotes = false;
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (ch == ',' && !inQuotes) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        parts.add(current.toString());
        return parts;
    }

    private String sanitizeFileName(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String normalizeBoxId(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private void recreateDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (var walk = Files.walk(directory)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
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
        }
        Files.createDirectories(directory);
    }

    public record ExperimentReport(Path resultCsvPath, Path sampleDir, ExperimentStats stats, int sampledOrderCount) {
    }

    public static class ExperimentStats {
        private final int cleanedItemCount;
        private final int cleanedBoxCount;
        private final long totalOrderCount;
        private final int sampledOrderCount;
        private long processedOrders;
        private long okOrders;
        private long missingItemOrders;
        private long missingBoxOrders;
        private long multipleBoxOrders;
        private long noFeasibleBoxOrders;
        private long multipleInnerBoxOrders;
        private long validOrdersForSampling;

        public ExperimentStats(int cleanedItemCount, int cleanedBoxCount, long totalOrderCount, int sampledOrderCount) {
            this.cleanedItemCount = cleanedItemCount;
            this.cleanedBoxCount = cleanedBoxCount;
            this.totalOrderCount = totalOrderCount;
            this.sampledOrderCount = sampledOrderCount;
        }

        private void accept(String status) {
            switch (status) {
                case "OK" -> okOrders++;
                case "ITEM_MISSING" -> missingItemOrders++;
                case "ACTUAL_BOX_MISSING" -> missingBoxOrders++;
                case "MULTIPLE_BOX_IDS" -> multipleBoxOrders++;
                case "NO_FEASIBLE_BOX" -> noFeasibleBoxOrders++;
                case "MULTIPLE_INNER_BOX_ITEMS" -> multipleInnerBoxOrders++;
                default -> { }
            }
        }

        public int cleanedItemCount() {
            return cleanedItemCount;
        }

        public int cleanedBoxCount() {
            return cleanedBoxCount;
        }

        public long totalOrderCount() {
            return totalOrderCount;
        }

        public int sampledOrderCount() {
            return sampledOrderCount;
        }

        public long processedOrders() {
            return processedOrders;
        }

        public long okOrders() {
            return okOrders;
        }

        public long missingItemOrders() {
            return missingItemOrders;
        }

        public long missingBoxOrders() {
            return missingBoxOrders;
        }

        public long multipleBoxOrders() {
            return multipleBoxOrders;
        }

        public long noFeasibleBoxOrders() {
            return noFeasibleBoxOrders;
        }

        public long multipleInnerBoxOrders() {
            return multipleInnerBoxOrders;
        }
    }

    private record Catalog(Map<String, ItemEntry> itemCatalog,
                           Map<String, BoxEntry> boxCatalog,
                           List<BoxEntry> sortedBoxes,
                           int cleanedItemCount,
                           int cleanedBoxCount) {
    }

    private record ItemEntry(String itemId, Item item) {
    }

    private record BoxEntry(String boxId, ContainerBox containerBox) {
        private BigInteger volume() {
            return BigInteger.valueOf(containerBox.getSizeX())
                    .multiply(BigInteger.valueOf(containerBox.getSizeY()))
                    .multiply(BigInteger.valueOf(containerBox.getSizeZ()));
        }
    }

    private record AlgorithmDecision(String boxId, BigDecimal utilization, String status) {
    }

    private record EvaluationResult(String orderId,
                                    String actualBoxId,
                                    BigDecimal actualUtilization,
                                    String algorithmBoxId,
                                    BigDecimal algorithmUtilization,
                                    String status,
                                    SampleOrder sampleOrder,
                                    boolean includeInComparison) {
        private static EvaluationResult invalid(String orderId, String actualBoxId, String status) {
            return new EvaluationResult(
                    orderId,
                    actualBoxId,
                    BigDecimal.ZERO.setScale(UTILIZATION_SCALE, RoundingMode.HALF_UP),
                    "NONE",
                    BigDecimal.ZERO.setScale(UTILIZATION_SCALE, RoundingMode.HALF_UP),
                    status,
                    null,
                    true
            );
        }

        private static EvaluationResult skipped(String orderId, String actualBoxId, String status) {
            return new EvaluationResult(
                    orderId,
                    actualBoxId,
                    BigDecimal.ZERO.setScale(UTILIZATION_SCALE, RoundingMode.HALF_UP),
                    "NONE",
                    BigDecimal.ZERO.setScale(UTILIZATION_SCALE, RoundingMode.HALF_UP),
                    status,
                    null,
                    false
            );
        }

        private String toCsvRow() {
            return quoteCsv(orderId) + ","
                    + quoteCsv(actualBoxId) + ","
                    + actualUtilization.toPlainString() + ","
                    + quoteCsv(algorithmBoxId) + ","
                    + algorithmUtilization.toPlainString() + ","
                    + quoteCsv(status);
        }
    }

    private record SampleOrder(String orderId, String actualBoxId, String algorithmBoxId, List<Item> items) {
        private SampleOrder {
            items = List.copyOf(items);
        }
    }

    private record SampledOrders(List<OrderBlock> orderBlocks, long totalOrderCount) {
        private SampledOrders {
            orderBlocks = List.copyOf(orderBlocks);
        }
    }

    private static class ReservoirSampler<T> {
        private final int capacity;
        private final Random random;
        private final List<T> samples = new ArrayList<>();

        private ReservoirSampler(int capacity, Random random) {
            this.capacity = capacity;
            this.random = Objects.requireNonNull(random, "random");
        }

        private void consider(T sample, long seenCount) {
            if (samples.size() < capacity) {
                samples.add(sample);
                return;
            }
            long candidateIndex = Math.floorMod(random.nextLong(), seenCount + 1);
            if (candidateIndex < capacity) {
                samples.set((int) candidateIndex, sample);
            }
        }

        private List<T> getSamples() {
            return List.copyOf(samples);
        }
    }

    private static class OrderBlockBuilder {
        private final String orderId;
        private final Map<String, Integer> itemQuantities = new LinkedHashMap<>();
        private String firstBoxId;
        private boolean multipleBoxes;

        private OrderBlockBuilder(String orderId) {
            this.orderId = orderId;
        }

        private boolean isSameOrder(String otherOrderId) {
            return orderId.equals(otherOrderId);
        }

        private void addLine(String itemId, int quantity, String boxId) {
            itemQuantities.merge(itemId, quantity, Integer::sum);
            if (firstBoxId == null) {
                firstBoxId = boxId;
            } else if (!firstBoxId.equals(boxId)) {
                multipleBoxes = true;
            }
        }

        private OrderBlock build() {
            return new OrderBlock(orderId, firstBoxId == null ? "" : firstBoxId, itemQuantities, multipleBoxes);
        }
    }

    private record OrderBlock(String orderId, String firstBoxId, Map<String, Integer> itemQuantities, boolean multipleBoxes) {
        private boolean hasSingleActualBox() {
            return !multipleBoxes;
        }
    }

    private static String quoteCsv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
