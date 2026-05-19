package com.zeroffa.tdbinpacking.service;

import com.zeroffa.tdbinpacking.model.ContainerBox;
import com.zeroffa.tdbinpacking.model.Item;
import com.zeroffa.tdbinpacking.model.PackingResult;
import com.zeroffa.tdbinpacking.model.Placement;
import com.zeroffa.tdbinpacking.solver.ExtremePointBinPacker;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class CorePackerAdapter {

    private final ExtremePointBinPacker packer;

    CorePackerAdapter(ExtremePointBinPacker packer) {
        this.packer = packer;
    }

    SingleBoxPackingAttempt pack(ContainerCandidate container, List<SchedulableItem> items) {
        Map<String, SchedulableItem> itemsByRuntimeId = new LinkedHashMap<>();
        List<Item> algorithmItems = items.stream()
                .map(item -> {
                    itemsByRuntimeId.put(item.runtimeId(), item);
                    return new Item(item.runtimeId(), item.sizeX(), item.sizeY(), item.sizeZ(), item.allowDown());
                })
                .collect(Collectors.toList());

        PackingResult result = packer.pack(
                new ContainerBox(container.sizeX(), container.sizeY(), container.sizeZ()),
                algorithmItems
        );

        List<PackedItem> packedItems = result.getPlacements().stream()
                .map(placement -> toPackedItem(placement, itemsByRuntimeId))
                .collect(Collectors.toList());
        List<SchedulableItem> geometryUnplacedItems = result.getUnplacedItems().stream()
                .map(item -> itemsByRuntimeId.get(item.getId()))
                .collect(Collectors.toList());

        WeightAdjustedPacking adjustedPacking = applyWeightLimit(container, packedItems, geometryUnplacedItems);
        return new SingleBoxPackingAttempt(container,
                adjustedPacking.packedItems,
                adjustedPacking.unplacedItems,
                adjustedPacking.unplacedItems.isEmpty(),
                adjustedPacking.packedVolume,
                result.getContainerVolume(),
                result.getContainerVolume() == 0L ? 0.0D : (double) adjustedPacking.packedVolume / result.getContainerVolume());
    }

    private PackedItem toPackedItem(Placement placement, Map<String, SchedulableItem> itemsByRuntimeId) {
        SchedulableItem item = itemsByRuntimeId.get(placement.getItem().getId());
        return new PackedItem(item, placement);
    }

    private WeightAdjustedPacking applyWeightLimit(ContainerCandidate container,
                                                   List<PackedItem> packedItems,
                                                   List<SchedulableItem> geometryUnplacedItems) {
        if (container.maxWeight() == null) {
            return new WeightAdjustedPacking(packedItems, geometryUnplacedItems, packedVolume(packedItems));
        }

        List<PackedItem> acceptedItems = new ArrayList<PackedItem>();
        List<SchedulableItem> rejectedItems = new ArrayList<SchedulableItem>();
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (PackedItem packedItem : packedItems) {
            BigDecimal nextWeight = totalWeight.add(packedItem.item().totalWeight());
            if (nextWeight.compareTo(container.maxWeight()) <= 0) {
                acceptedItems.add(packedItem);
                totalWeight = nextWeight;
            } else {
                rejectedItems.add(packedItem.item());
            }
        }

        rejectedItems.addAll(geometryUnplacedItems);
        return new WeightAdjustedPacking(acceptedItems, rejectedItems, packedVolume(acceptedItems));
    }

    private long packedVolume(List<PackedItem> packedItems) {
        long total = 0L;
        for (PackedItem packedItem : packedItems) {
            total += packedItem.item().volume();
        }
        return total;
    }

    private static final class WeightAdjustedPacking {
        private final List<PackedItem> packedItems;
        private final List<SchedulableItem> unplacedItems;
        private final long packedVolume;

        private WeightAdjustedPacking(List<PackedItem> packedItems,
                                      List<SchedulableItem> unplacedItems,
                                      long packedVolume) {
            this.packedItems = packedItems;
            this.unplacedItems = unplacedItems;
            this.packedVolume = packedVolume;
        }
    }
}
