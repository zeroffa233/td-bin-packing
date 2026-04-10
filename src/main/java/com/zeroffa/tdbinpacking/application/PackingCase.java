package com.zeroffa.tdbinpacking.application;

import com.zeroffa.tdbinpacking.model.ContainerBox;
import com.zeroffa.tdbinpacking.model.Item;

import java.util.List;

public class PackingCase {

    private final String caseId;
    private final ContainerBox containerBox;
    private final List<Item> items;

    public PackingCase(String caseId, ContainerBox containerBox, List<Item> items) {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId must not be blank");
        }
        if (containerBox == null) {
            throw new IllegalArgumentException("containerBox must not be null");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        this.caseId = caseId;
        this.containerBox = containerBox;
        this.items = List.copyOf(items);
    }

    public String getCaseId() {
        return caseId;
    }

    public ContainerBox getContainerBox() {
        return containerBox;
    }

    public List<Item> getItems() {
        return items;
    }
}
