package com.zeroffa.tdbinpacking.application.io;

import com.zeroffa.tdbinpacking.model.Item;
import com.zeroffa.tdbinpacking.model.ItemAttribute;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ItemCsvReader {

    public List<Item> read(Path csvPath) throws IOException {
        List<Item> items = new ArrayList<>();
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
                if (cols.size() != 4 && cols.size() != 5) {
                    throw new IllegalArgumentException("Invalid item csv column size at line " + lineNumber + ": expected 4 or 5");
                }
                String itemId = cols.get(0).trim();
                int itemX = parsePositiveInt(cols.get(1), "itemX", lineNumber);
                int itemY = parsePositiveInt(cols.get(2), "itemY", lineNumber);
                int itemZ = parsePositiveInt(cols.get(3), "itemZ", lineNumber);
                ItemAttribute attribute = cols.size() == 5
                        ? ItemAttribute.fromCsvValue(cols.get(4))
                        : ItemAttribute.PRODUCT;
                items.add(new Item(itemId, itemX, itemY, itemZ, attribute));
            }
        }
        return items;
    }

    private boolean isHeader(List<String> cols) {
        return !cols.isEmpty() && "itemId".equalsIgnoreCase(cols.get(0).trim());
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
        List<String> parts = new ArrayList<>(4);
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
