package com.zeroffa.tdbinpacking.model;

public enum ItemAttribute {
    PRODUCT("product"),
    BOX("box"),
    BAG("bag");

    private final String csvValue;

    ItemAttribute(String csvValue) {
        this.csvValue = csvValue;
    }

    public String getCsvValue() {
        return csvValue;
    }

    public static ItemAttribute fromCsvValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return PRODUCT;
        }
        return switch (rawValue.trim().toLowerCase()) {
            case "bag" -> BAG;
            case "box" -> BOX;
            default -> PRODUCT;
        };
    }
}
