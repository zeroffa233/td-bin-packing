package com.zeroffa.tdbinpacking.application;

import com.zeroffa.tdbinpacking.model.Item;

import java.util.List;

public class PackingPreparation {

    private final List<Item> preparedItems;
    private final List<Item> droppedBagItems;
    private final List<Item> boxedProductItems;
    private final Item innerBoxItem;

    public PackingPreparation(List<Item> preparedItems,
                              List<Item> droppedBagItems,
                              List<Item> boxedProductItems,
                              Item innerBoxItem) {
        this.preparedItems = List.copyOf(preparedItems);
        this.droppedBagItems = List.copyOf(droppedBagItems);
        this.boxedProductItems = List.copyOf(boxedProductItems);
        this.innerBoxItem = innerBoxItem;
    }

    public List<Item> getPreparedItems() {
        return preparedItems;
    }

    public List<Item> getDroppedBagItems() {
        return droppedBagItems;
    }

    public List<Item> getBoxedProductItems() {
        return boxedProductItems;
    }

    public Item getInnerBoxItem() {
        return innerBoxItem;
    }

    public long getPreparedVolume() {
        return preparedItems.stream().mapToLong(Item::volume).sum();
    }
}
