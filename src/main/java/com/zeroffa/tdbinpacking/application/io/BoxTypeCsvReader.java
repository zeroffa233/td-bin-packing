package com.zeroffa.tdbinpacking.application.io;

import com.zeroffa.tdbinpacking.application.BoxType;
import com.zeroffa.tdbinpacking.model.ContainerBox;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BoxTypeCsvReader {

    public List<BoxType> read(Path csvPath) throws IOException {
        List<BoxType> boxTypes = new ArrayList<>();
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
                if (cols.size() != 4) {
                    throw new IllegalArgumentException("Invalid box type csv column size at line " + lineNumber + ": expected 4");
                }
                String boxTypeId = cols.get(0).trim();
                int boxX = parsePositiveInt(cols.get(1), "containerX", lineNumber);
                int boxY = parsePositiveInt(cols.get(2), "containerY", lineNumber);
                int boxZ = parsePositiveInt(cols.get(3), "containerZ", lineNumber);
                boxTypes.add(new BoxType(boxTypeId, new ContainerBox(boxX, boxY, boxZ)));
            }
        }
        return boxTypes;
    }

    private boolean isHeader(List<String> cols) {
        return !cols.isEmpty() && "boxTypeId".equalsIgnoreCase(cols.get(0).trim());
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
