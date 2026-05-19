package com.zeroffa.tdbinpacking.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        List<PackedContainer> packedContainers = new ArrayList<PackedContainer>();

        while (!remaining.isEmpty()) {
            SingleBoxPackingAttempt selected = findFullPlacement(remaining.items(), candidates);
            if (selected == null) {
                selected = findBestPartialPlacement(remaining.items(), candidates);
            }
            if (selected == null || selected.packedItems().isEmpty()) {
                return new GroupPackingPlan(groupId, packedContainers, true,
                        buildFailureMessage(groupId, remaining.items(), candidates));
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

    private String buildFailureMessage(int groupId,
                                       List<SchedulableItem> remainingItems,
                                       List<ContainerCandidate> candidates) {
        List<ContainerCandidate> availableCandidates = candidates.stream()
                .filter(ContainerCandidate::hasInventory)
                .collect(Collectors.toList());
        String itemSummary = formatItemSummary(remainingItems);
        if (availableCandidates.isEmpty()) {
            return "group " + groupId + " 装箱失败：可用箱型库存已耗尽，剩余商品=" + itemSummary;
        }

        boolean anyGeometryPlacement = false;
        boolean hasWeightViolation = false;
        boolean hasCapacityViolation = false;

        for (ContainerCandidate candidate : availableCandidates) {
            SingleBoxPackingAttempt attempt = corePacker.pack(candidate, remainingItems);
            if (!attempt.packedItems().isEmpty()) {
                anyGeometryPlacement = true;
                if (!legalityValidator.isWeightLegal(attempt)) {
                    hasWeightViolation = true;
                }
                if (!legalityValidator.isCapacityLegal(attempt)) {
                    hasCapacityViolation = true;
                }
            }
        }

        if (!anyGeometryPlacement) {
            return "group " + groupId + " 装箱失败：剩余商品无法放入任何可用箱型，剩余商品=" + itemSummary;
        }
        if (hasWeightViolation && hasCapacityViolation) {
            return "group " + groupId + " 装箱失败：剩余商品存在超重且超容积约束，剩余商品=" + itemSummary;
        }
        if (hasWeightViolation) {
            return "group " + groupId + " 装箱失败：剩余商品受最大承重限制无法装入单箱，剩余商品=" + itemSummary;
        }
        if (hasCapacityViolation) {
            return "group " + groupId + " 装箱失败：剩余商品受容积利用率限制无法装入单箱，剩余商品=" + itemSummary;
        }
        return "group " + groupId + " 装箱失败：未找到合法箱型组合，剩余商品=" + itemSummary;
    }

    private String formatItemSummary(List<SchedulableItem> items) {
        Map<String, Long> quantities = new LinkedHashMap<String, Long>();
        for (SchedulableItem item : items) {
            for (SchedulableItem represented : item.representedItems()) {
                String itemCode = represented.itemCode();
                Long current = quantities.get(itemCode);
                quantities.put(itemCode, current == null ? 1L : current + 1L);
            }
        }
        List<String> parts = new ArrayList<String>();
        for (Map.Entry<String, Long> entry : quantities.entrySet()) {
            parts.add(entry.getKey() + "*" + entry.getValue());
        }
        return "{" + join(parts, ",") + "}";
    }

    private String join(List<String> values, String separator) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(separator);
            }
            builder.append(values.get(index));
        }
        return builder.toString();
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
