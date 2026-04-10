package com.zeroffa.tdbinpacking.model;

public interface AxisAlignedBox {

    int getX();

    int getY();

    int getZ();

    int getSizeX();

    int getSizeY();

    int getSizeZ();

    default int getMaxX() {
        return getX() + getSizeX();
    }

    default int getMaxY() {
        return getY() + getSizeY();
    }

    default int getMaxZ() {
        return getZ() + getSizeZ();
    }
}
