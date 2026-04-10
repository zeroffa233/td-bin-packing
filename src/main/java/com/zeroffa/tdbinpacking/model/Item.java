package com.zeroffa.tdbinpacking.model;

public class Item {

    private final String id;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final ItemAttribute attribute;

    public Item(String id, int sizeX, int sizeY, int sizeZ) {
        this(id, sizeX, sizeY, sizeZ, ItemAttribute.PRODUCT);
    }

    public Item(String id, int sizeX, int sizeY, int sizeZ, ItemAttribute attribute) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        validatePositive(sizeX, "sizeX");
        validatePositive(sizeY, "sizeY");
        validatePositive(sizeZ, "sizeZ");
        if (attribute == null) {
            throw new IllegalArgumentException("attribute must not be null");
        }
        this.id = id;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.attribute = attribute;
    }

    public String getId() {
        return id;
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int getSizeZ() {
        return sizeZ;
    }

    public ItemAttribute getAttribute() {
        return attribute;
    }

    public long volume() {
        return (long) sizeX * sizeY * sizeZ;
    }

    private static void validatePositive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    @Override
    public String toString() {
        return String.format("%s[%d,%d,%d,%s]", id, sizeX, sizeY, sizeZ, attribute.getCsvValue());
    }
}
