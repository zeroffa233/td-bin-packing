package com.zeroffa.tdbinpacking.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

class TraditionalMultiBoxPlanner {

    private final CorePackerAdapter corePacker;
    private final RuntimeLegalityValidator legalityValidator;

    TraditionalMultiBoxPlanner(CorePackerAdapter corePacker, RuntimeLegalityValidator legalityValidator) {
        this.corePacker = corePacker;
        this.legalityValidator = legalityValidator;
    }

    GroupPackingPlan packAll(int groupId, List<SchedulableItem> items, List<ContainerCandidate> candidates) {
        GroupItemBucket remaining = new GroupItemBucket(items);
        List<PackedContainer> packedContainers = new ArrayList<>();

        while (!remaining.isEmpty()) {
            SingleBoxPackingAttempt selected = findFullPlacement(remaining.items(), candidates);
            if (selected == null) {
                selected = findBestPartialPlacement(remaining.items(), candidates);
            }
            if (selected == null || selected.packedItems().isEmpty()) {
                return new GroupPackingPlan(groupId, packedContainers, true,
                        "no legal container can pack remaining items in group " + groupId);
            }
            lockAttempt(groupId, selected, packedContainers, remaining);
        }

        return new GroupPackingPlan(groupId, Collections.unmodifiableList(new ArrayList<PackedContainer>(packedContainers)), false, "");
    }

    private SingleBoxPackingAttempt findFullPlacement(List<SchedulableItem> items,
                                                     List<ContainerCandidate> candidates) {
        for (ContainerCandidate candidate : candidates) {
            if (!candidate.hasInventory()) {
                continue;
            }
            SingleBoxPackingAttempt attempt = corePacker.pack(candidate, items);
            if (attempt.allPlaced() && legalityValidator.isLegal(attempt)) {
                return attempt;
            }
        }
        return null;
    }

    private SingleBoxPackingAttempt findBestPartialPlacement(List<SchedulableItem> items,
                                                            List<ContainerCandidate> candidates) {
        return candidates.stream()
                .filter(ContainerCandidate::hasInventory)
                .map(candidate -> corePacker.pack(candidate, items))
                .filter(attempt -> !attempt.packedItems().isEmpty())
                .filter(legalityValidator::isLegal)
                .max(Comparator.comparingInt((SingleBoxPackingAttempt attempt) -> attempt.packedItems().size())
                        .thenComparingLong(SingleBoxPackingAttempt::packedVolume))
                .orElse(null);
    }

    private void lockAttempt(int groupId,
                             SingleBoxPackingAttempt attempt,
                             List<PackedContainer> packedContainers,
                             GroupItemBucket remaining) {
        packedContainers.add(new PackedContainer(groupId, attempt.container(), attempt.packedItems()));
        attempt.container().decrement();
        remaining.removeAll(attempt.packedItems().stream().map(PackedItem::item).collect(Collectors.toList()));
    }
}
