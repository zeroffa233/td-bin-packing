package com.zeroffa.tdbinpacking.model;

public class ContainerBox {

    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    public ContainerBox(int sizeX, int sizeY, int sizeZ) {
        validatePositive(sizeX, "sizeX");
        validatePositive(sizeY, "sizeY");
        validatePositive(sizeZ, "sizeZ");
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
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
        return String.format("[%d, %d, %d]", sizeX, sizeY, sizeZ);
    }
}
