package com.zeroffa.tdbinpacking.service;

import com.zeroffa.tdbinpacking.model.ContainerBox;
import com.zeroffa.tdbinpacking.model.Item;
import com.zeroffa.tdbinpacking.model.PackingResult;
import com.zeroffa.tdbinpacking.model.Placement;
import com.zeroffa.tdbinpacking.solver.ExtremePointBinPacker;

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
        List<SchedulableItem> unplacedItems = result.getUnplacedItems().stream()
                .map(item -> itemsByRuntimeId.get(item.getId()))
                .collect(Collectors.toList());

        return new SingleBoxPackingAttempt(container, packedItems, unplacedItems, result.isAllPlaced(),
                result.getPackedVolume(), result.getContainerVolume(), result.getUtilization());
    }

    private PackedItem toPackedItem(Placement placement, Map<String, SchedulableItem> itemsByRuntimeId) {
        SchedulableItem item = itemsByRuntimeId.get(placement.getItem().getId());
        return new PackedItem(item, placement);
    }
}
