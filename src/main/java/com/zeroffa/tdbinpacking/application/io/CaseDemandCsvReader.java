package com.zeroffa.tdbinpacking.application.io;

import com.zeroffa.tdbinpacking.application.CaseDemand;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CaseDemandCsvReader {

    public List<CaseDemand> read(Path csvPath) throws IOException {
        Map<String, Map<String, Integer>> caseMap = new LinkedHashMap<>();
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
                if (cols.size() != 3) {
                    throw new IllegalArgumentException("Invalid cases csv column size at line " + lineNumber + ": expected 3");
                }

                String caseId = cols.get(0).trim();
                String itemId = cols.get(1).trim();
                int quantity = parsePositiveInt(cols.get(2), "quantity", lineNumber);

                if (caseId.isBlank()) {
                    throw new IllegalArgumentException("caseId must not be blank at line " + lineNumber);
                }
                if (itemId.isBlank()) {
                    throw new IllegalArgumentException("itemId must not be blank at line " + lineNumber);
                }

                Map<String, Integer> itemQuantities = caseMap.computeIfAbsent(caseId, key -> new LinkedHashMap<>());
                itemQuantities.merge(itemId, quantity, Integer::sum);
            }
        }

        List<CaseDemand> demands = new ArrayList<>(caseMap.size());
        for (Map.Entry<String, Map<String, Integer>> entry : caseMap.entrySet()) {
            demands.add(new CaseDemand(entry.getKey(), entry.getValue()));
        }
        return demands;
    }

    private boolean isHeader(List<String> cols) {
        return !cols.isEmpty() && "caseId".equalsIgnoreCase(cols.get(0).trim());
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
        List<String> parts = new ArrayList<>(3);
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
}
