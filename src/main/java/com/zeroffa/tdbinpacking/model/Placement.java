package com.zeroffa.tdbinpacking.model;

public class Placement implements AxisAlignedBox {

    private final Item item;
    private final Orientation orientation;
    private final int x;
    private final int y;
    private final int z;

    public Placement(Item item, Orientation orientation, int x, int y, int z) {
        this.item = item;
        this.orientation = orientation;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Item getItem() {
        return item;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getZ() {
        return z;
    }

    @Override
    public int getSizeX() {
        return orientation.getSizeX();
    }

    @Override
    public int getSizeY() {
        return orientation.getSizeY();
    }

    @Override
    public int getSizeZ() {
        return orientation.getSizeZ();
    }

    public long volume() {
        return (long) getSizeX() * getSizeY() * getSizeZ();
    }

    public String toMatrixRow() {
        return String.format("item=%s, origin=[%d,%d,%d], size=[%d,%d,%d]",
                item.getId(), x, y, z, getSizeX(), getSizeY(), getSizeZ());
    }

    @Override
    public String toString() {
        return toMatrixRow();
    }
}
