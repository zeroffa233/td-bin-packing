package com.zeroffa.tdbinpacking.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PackingResult {

    private final ContainerBox containerBox;
    private final List<Placement> placements;
    private final List<Item> unplacedItems;

    public PackingResult(ContainerBox containerBox, List<Placement> placements, List<Item> unplacedItems) {
        this.containerBox = containerBox;
        this.placements = Collections.unmodifiableList(new ArrayList<Placement>(placements));
        this.unplacedItems = Collections.unmodifiableList(new ArrayList<Item>(unplacedItems));
    }

    public ContainerBox getContainerBox() {
        return containerBox;
    }

    public List<Placement> getPlacements() {
        return Collections.unmodifiableList(placements);
    }

    public List<Item> getUnplacedItems() {
        return Collections.unmodifiableList(unplacedItems);
    }

    public long getPackedVolume() {
        return placements.stream().mapToLong(Placement::volume).sum();
    }

    public long getContainerVolume() {
        return containerBox.volume();
    }

    public double getUtilization() {
        return getContainerVolume() == 0L ? 0.0D : (double) getPackedVolume() / getContainerVolume();
    }

    public boolean isAllPlaced() {
        return unplacedItems.isEmpty();
    }
}
