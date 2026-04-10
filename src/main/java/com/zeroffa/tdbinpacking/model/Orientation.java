package com.zeroffa.tdbinpacking.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Orientation {

    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    public Orientation(int sizeX, int sizeY, int sizeZ) {
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

    public static List<Orientation> generate(Item item) {
        int x = item.getSizeX();
        int y = item.getSizeY();
        int z = item.getSizeZ();
        Set<Orientation> set = new LinkedHashSet<>();
        set.add(new Orientation(x, y, z));
        set.add(new Orientation(x, z, y));
        set.add(new Orientation(y, x, z));
        set.add(new Orientation(y, z, x));
        set.add(new Orientation(z, x, y));
        set.add(new Orientation(z, y, x));
        return new ArrayList<>(set);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Orientation that)) {
            return false;
        }
        return sizeX == that.sizeX && sizeY == that.sizeY && sizeZ == that.sizeZ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sizeX, sizeY, sizeZ);
    }

    @Override
    public String toString() {
        return String.format("[%d,%d,%d]", sizeX, sizeY, sizeZ);
    }
}
