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
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return PRODUCT;
        }
        String normalized = rawValue.trim().toLowerCase();
        if ("bag".equals(normalized)) {
            return BAG;
        }
        if ("box".equals(normalized)) {
            return BOX;
        }
        return PRODUCT;
    }
}
