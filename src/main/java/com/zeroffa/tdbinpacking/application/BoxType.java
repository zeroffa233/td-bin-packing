package com.zeroffa.tdbinpacking.application;

import com.zeroffa.tdbinpacking.model.ContainerBox;

public class BoxType {

    private final String boxTypeId;
    private final ContainerBox containerBox;

    public BoxType(String boxTypeId, ContainerBox containerBox) {
        if (boxTypeId == null || boxTypeId.isBlank()) {
            throw new IllegalArgumentException("boxTypeId must not be blank");
        }
        if (containerBox == null) {
            throw new IllegalArgumentException("containerBox must not be null");
        }
        this.boxTypeId = boxTypeId;
        this.containerBox = containerBox;
    }

    public String getBoxTypeId() {
        return boxTypeId;
    }

    public ContainerBox getContainerBox() {
        return containerBox;
    }
}
