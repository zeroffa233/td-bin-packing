package com.zeroffa.tdbinpacking.application;

import com.zeroffa.tdbinpacking.model.ContainerBox;
import com.zeroffa.tdbinpacking.model.Item;
import com.zeroffa.tdbinpacking.model.ItemAttribute;
import com.zeroffa.tdbinpacking.model.PackingResult;
import com.zeroffa.tdbinpacking.solver.ExtremePointBinPacker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class PackingInputPreprocessor {

    private final ExtremePointBinPacker packer;

    public PackingInputPreprocessor(ExtremePointBinPacker packer) {
        this.packer = Objects.requireNonNull(packer, "packer");
    }

    public PackingPreparation prepare(List<Item> inputItems) {
        Objects.requireNonNull(inputItems, "inputItems");

        List<Item> bagItems = new ArrayList<>();
        List<Item> boxItems = new ArrayList<>();
        List<Item> productItems = new ArrayList<>();

        for (Item item : inputItems) {
            if (item.getAttribute() == ItemAttribute.BAG) {
                bagItems.add(item);
            } else if (item.getAttribute() == ItemAttribute.BOX) {
                boxItems.add(item);
            } else {
                productItems.add(item);
            }
        }

        if (boxItems.size() > 1) {
            throw new IllegalArgumentException("Multiple box items found in one packing input: " + boxItems.size());
        }

        List<Item> sortedProducts = productItems.stream()
                .sorted(Comparator.comparingLong(Item::volume).reversed().thenComparing(Item::getId))
                .toList();

        if (boxItems.isEmpty()) {
            return new PackingPreparation(sortedProducts, bagItems, List.of(), null);
        }

        Item innerBoxItem = boxItems.get(0);
        ContainerBox innerBox = new ContainerBox(innerBoxItem.getSizeX(), innerBoxItem.getSizeY(), innerBoxItem.getSizeZ());
        List<Item> boxedProducts = new ArrayList<>();
        List<Item> remainingProducts = new ArrayList<>();

        for (Item product : sortedProducts) {
            List<Item> trialItems = new ArrayList<>(boxedProducts.size() + 1);
            trialItems.addAll(boxedProducts);
            trialItems.add(product);

            PackingResult trialResult = packer.pack(innerBox, trialItems);
            if (trialResult.isAllPlaced()) {
                boxedProducts.add(product);
            } else {
                remainingProducts.add(product);
            }
        }

        List<Item> preparedItems = new ArrayList<>(remainingProducts.size() + 1);
        preparedItems.add(innerBoxItem);
        preparedItems.addAll(remainingProducts);
        return new PackingPreparation(preparedItems, bagItems, boxedProducts, innerBoxItem);
    }
}
