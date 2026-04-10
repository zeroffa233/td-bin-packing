package com.zeroffa.tdbinpacking.application.io;

import com.zeroffa.tdbinpacking.application.PackingCase;
import com.zeroffa.tdbinpacking.model.ContainerBox;
import com.zeroffa.tdbinpacking.model.Item;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PackingCaseCsvReader {

    public List<PackingCase> read(Path csvPath) throws IOException {
        Map<String, MutableCase> caseMap = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                List<String> cols = splitCsvLine(line);
                if (lineNumber == 1 && isHeader(cols)) {
                    continue;
                }
                if (cols.size() != 8) {
                    throw new IllegalArgumentException("Invalid column size at line " + lineNumber + ": expected 8");
                }

                String caseId = cols.get(0).trim();
                int boxX = parsePositiveInt(cols.get(1), "containerX", lineNumber);
                int boxY = parsePositiveInt(cols.get(2), "containerY", lineNumber);
                int boxZ = parsePositiveInt(cols.get(3), "containerZ", lineNumber);
                String itemId = cols.get(4).trim();
                int itemX = parsePositiveInt(cols.get(5), "itemX", lineNumber);
                int itemY = parsePositiveInt(cols.get(6), "itemY", lineNumber);
                int itemZ = parsePositiveInt(cols.get(7), "itemZ", lineNumber);

                MutableCase mutableCase = caseMap.computeIfAbsent(caseId,
                        key -> new MutableCase(caseId, boxX, boxY, boxZ));
                mutableCase.assertContainerSize(boxX, boxY, boxZ, lineNumber);
                mutableCase.items.add(new Item(itemId, itemX, itemY, itemZ));
            }
        }

        List<PackingCase> cases = new ArrayList<>(caseMap.size());
        for (MutableCase mutableCase : caseMap.values()) {
            cases.add(new PackingCase(
                    mutableCase.caseId,
                    new ContainerBox(mutableCase.containerX, mutableCase.containerY, mutableCase.containerZ),
                    mutableCase.items
            ));
        }
        return cases;
    }

    private boolean isHeader(List<String> columns) {
        return !columns.isEmpty() && "caseId".equalsIgnoreCase(columns.get(0).trim());
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
        List<String> parts = new ArrayList<>(8);
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

    private static class MutableCase {
        private final String caseId;
        private final int containerX;
        private final int containerY;
        private final int containerZ;
        private final List<Item> items = new ArrayList<>();

        private MutableCase(String caseId, int containerX, int containerY, int containerZ) {
            if (caseId == null || caseId.isBlank()) {
                throw new IllegalArgumentException("caseId must not be blank");
            }
            this.caseId = caseId;
            this.containerX = containerX;
            this.containerY = containerY;
            this.containerZ = containerZ;
        }

        private void assertContainerSize(int boxX, int boxY, int boxZ, int lineNumber) {
            if (boxX != containerX || boxY != containerY || boxZ != containerZ) {
                throw new IllegalArgumentException("Container size mismatch in caseId '" + caseId
                        + "' at line " + lineNumber);
            }
        }
    }
}
