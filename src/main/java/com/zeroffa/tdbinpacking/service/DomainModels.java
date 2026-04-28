package com.zeroffa.tdbinpacking.service;

import com.zeroffa.tdbinpacking.api.LclRule;
import com.zeroffa.tdbinpacking.api.OrderDetail;
import com.zeroffa.tdbinpacking.api.RequestContainer;
import com.zeroffa.tdbinpacking.api.RequestItem;
import com.zeroffa.tdbinpacking.model.Placement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class ScaleConfig {
    private final int dimensionScale;

    ScaleConfig(int dimensionScale) {
        this.dimensionScale = dimensionScale;
    }

    static ScaleConfig defaultConfig() {
        return new ScaleConfig(10_000);
    }

    int dimensionScale() {
        return dimensionScale;
    }
}

final class NormalizedRequest {
    private final Map<Integer, PackingGroup> groups;
    private final List<ContainerCandidate> containers;
    private final int carrierService;
    private final List<LclRuleSpec> lclRules;
    private final int unpackJudge;

    NormalizedRequest(Map<Integer, PackingGroup> groups, List<ContainerCandidate> containers,
                      int carrierService, List<LclRuleSpec> lclRules, int unpackJudge) {
        this.groups = groups;
        this.containers = containers;
        this.carrierService = carrierService;
        this.lclRules = lclRules;
        this.unpackJudge = unpackJudge;
    }

    Map<Integer, PackingGroup> groups() { return groups; }
    List<ContainerCandidate> containers() { return containers; }
    int carrierService() { return carrierService; }
    List<LclRuleSpec> lclRules() { return lclRules; }
    int unpackJudge() { return unpackJudge; }
}

final class PackingGroup {
    private final int groupId;
    private final List<SchedulableItem> items;
    private final String stores;

    PackingGroup(int groupId, List<SchedulableItem> items, String stores) {
        this.groupId = groupId;
        this.items = items;
        this.stores = stores;
    }

    int groupId() { return groupId; }
    List<SchedulableItem> items() { return items; }
    String stores() { return stores; }
}

final class SchedulableItem {
    private final String runtimeId;
    private final String itemCode;
    private final long quantityUnitIndex;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final BigDecimal weight;
    private final int allowDown;
    private final String itemCategory1;
    private final String itemCategory2;
    private final String itemStyle;
    private final String batch;
    private final String lot;
    private final String manufactureDate;
    private final String buildCode;
    private final String zoneCode;
    private final String stores;
    private final OrderDetail source;
    private final List<SchedulableItem> representedItems;

    SchedulableItem(String runtimeId, String itemCode, long quantityUnitIndex, int sizeX, int sizeY, int sizeZ,
                    BigDecimal weight, int allowDown, String itemCategory1, String itemCategory2,
                    String itemStyle, String batch, String lot, String manufactureDate, String buildCode,
                    String zoneCode, String stores, OrderDetail source, List<SchedulableItem> representedItems) {
        this.runtimeId = runtimeId;
        this.itemCode = itemCode;
        this.quantityUnitIndex = quantityUnitIndex;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.weight = weight;
        this.allowDown = allowDown;
        this.itemCategory1 = itemCategory1;
        this.itemCategory2 = itemCategory2;
        this.itemStyle = itemStyle;
        this.batch = batch;
        this.lot = lot;
        this.manufactureDate = manufactureDate;
        this.buildCode = buildCode;
        this.zoneCode = zoneCode;
        this.stores = stores;
        this.source = source;
        this.representedItems = Collections.unmodifiableList(new ArrayList<SchedulableItem>(representedItems));
    }

    static SchedulableItem unit(String runtimeId, long quantityUnitIndex, RequestItem item,
                                OrderDetail source, int sizeX, int sizeY, int sizeZ) {
        BigDecimal weight = item.weight() == null ? BigDecimal.ZERO : item.weight();
        SchedulableItem unit = new SchedulableItem(runtimeId, item.code(), quantityUnitIndex,
                sizeX, sizeY, sizeZ, weight, item.allowDown(), item.itemCategory1(), item.itemCategory2(),
                item.itemStyle(), source.batch(), source.lot(), source.manufactureDate(), source.buildCode(),
                source.zoneCode(), source.stores(), source, Collections.<SchedulableItem>emptyList());
        return unit.withRepresentedItems(null);
    }

    SchedulableItem withRepresentedItems(List<SchedulableItem> represented) {
        List<SchedulableItem> next = represented == null
                ? Collections.singletonList(this)
                : new ArrayList<SchedulableItem>(represented);
        return new SchedulableItem(runtimeId, itemCode, quantityUnitIndex, sizeX, sizeY, sizeZ, weight, allowDown,
                itemCategory1, itemCategory2, itemStyle, batch, lot, manufactureDate, buildCode, zoneCode, stores,
                source, next);
    }

    BigDecimal totalWeight() {
        BigDecimal total = BigDecimal.ZERO;
        for (SchedulableItem item : representedItems) {
            total = total.add(item.weight());
        }
        return total;
    }

    long volume() {
        return (long) sizeX * sizeY * sizeZ;
    }

    String runtimeId() { return runtimeId; }
    String itemCode() { return itemCode; }
    int sizeX() { return sizeX; }
    int sizeY() { return sizeY; }
    int sizeZ() { return sizeZ; }
    BigDecimal weight() { return weight; }
    int allowDown() { return allowDown; }
    String itemCategory1() { return itemCategory1; }
    String itemStyle() { return itemStyle; }
    String batch() { return batch; }
    String lot() { return lot; }
    String manufactureDate() { return manufactureDate; }
    String buildCode() { return buildCode; }
    String zoneCode() { return zoneCode; }
    String stores() { return stores; }
    OrderDetail source() { return source; }
    List<SchedulableItem> representedItems() { return representedItems; }
}

final class ContainerCandidate {
    private final String code;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final BigDecimal maxWeight;
    private final BigDecimal capacityRate;
    private final int sequence;
    private long remainingQuantity;
    private final String stores;

    ContainerCandidate(String code, int sizeX, int sizeY, int sizeZ, BigDecimal maxWeight,
                       BigDecimal capacityRate, int sequence, long remainingQuantity, String stores) {
        this.code = code;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.maxWeight = maxWeight;
        this.capacityRate = capacityRate;
        this.sequence = sequence;
        this.remainingQuantity = remainingQuantity;
        this.stores = stores;
    }

    static ContainerCandidate from(RequestContainer container, int sizeX, int sizeY, int sizeZ) {
        return new ContainerCandidate(container.code(), sizeX, sizeY, sizeZ, container.maxWeight(),
                container.capacityRate(), container.sequence(), container.quantity(), container.stores());
    }

    String code() { return code; }
    int sizeX() { return sizeX; }
    int sizeY() { return sizeY; }
    int sizeZ() { return sizeZ; }
    BigDecimal maxWeight() { return maxWeight; }
    BigDecimal capacityRate() { return capacityRate; }
    int sequence() { return sequence; }
    long remainingQuantity() { return remainingQuantity; }
    String stores() { return stores; }
    long volume() { return (long) sizeX * sizeY * sizeZ; }
    boolean hasInventory() { return remainingQuantity > 0; }
    void decrement() { if (remainingQuantity > 0) remainingQuantity--; }
}

final class LclRuleSpec {
    private final int sequence;
    private final RuleCode code;
    private final String name;

    LclRuleSpec(int sequence, RuleCode code, String name) {
        this.sequence = sequence;
        this.code = code;
        this.name = name;
    }

    static LclRuleSpec from(LclRule rule) {
        return new LclRuleSpec(rule.sequence(), RuleCode.from(rule.code()), rule.name());
    }

    int sequence() { return sequence; }
    RuleCode code() { return code; }
    String name() { return name; }
}

enum RuleCode {
    SAME_SKU_LOT, SAME_SKU, SAME_TYPE, SAME_BUILD_ZONE, SAME_BUILD, SAME_ZONE, NO_LIMIT;

    static RuleCode from(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException("lclRule.code must not be blank");
        }
        return RuleCode.valueOf(value.trim().toUpperCase());
    }
}

final class SingleBoxPackingAttempt {
    private final ContainerCandidate container;
    private final List<PackedItem> packedItems;
    private final List<SchedulableItem> unplacedItems;
    private final boolean allPlaced;
    private final long packedVolume;
    private final long containerVolume;
    private final double utilization;

    SingleBoxPackingAttempt(ContainerCandidate container, List<PackedItem> packedItems,
                            List<SchedulableItem> unplacedItems, boolean allPlaced,
                            long packedVolume, long containerVolume, double utilization) {
        this.container = container;
        this.packedItems = packedItems;
        this.unplacedItems = unplacedItems;
        this.allPlaced = allPlaced;
        this.packedVolume = packedVolume;
        this.containerVolume = containerVolume;
        this.utilization = utilization;
    }

    ContainerCandidate container() { return container; }
    List<PackedItem> packedItems() { return packedItems; }
    List<SchedulableItem> unplacedItems() { return unplacedItems; }
    boolean allPlaced() { return allPlaced; }
    long packedVolume() { return packedVolume; }
    long containerVolume() { return containerVolume; }
    double utilization() { return utilization; }
}

final class PackedItem {
    private final SchedulableItem item;
    private final Placement placement;

    PackedItem(SchedulableItem item, Placement placement) {
        this.item = item;
        this.placement = placement;
    }

    SchedulableItem item() { return item; }
    Placement placement() { return placement; }
}

final class PackedContainer {
    private final int groupId;
    private final ContainerCandidate container;
    private final List<PackedItem> packedItems;

    PackedContainer(int groupId, ContainerCandidate container, List<PackedItem> packedItems) {
        this.groupId = groupId;
        this.container = container;
        this.packedItems = packedItems;
    }

    int groupId() { return groupId; }
    ContainerCandidate container() { return container; }
    List<PackedItem> packedItems() { return packedItems; }
}

final class GroupPackingPlan {
    private final int groupId;
    private final List<PackedContainer> containers;
    private final boolean failed;
    private final String message;

    GroupPackingPlan(int groupId, List<PackedContainer> containers, boolean failed, String message) {
        this.groupId = groupId;
        this.containers = containers;
        this.failed = failed;
        this.message = message;
    }

    int groupId() { return groupId; }
    List<PackedContainer> containers() { return containers; }
    boolean failed() { return failed; }
    String message() { return message; }
}

final class RequestPackingPlan {
    private final List<PackedContainer> containers;
    private final boolean failed;
    private final String message;

    RequestPackingPlan(List<PackedContainer> containers, boolean failed, String message) {
        this.containers = containers;
        this.failed = failed;
        this.message = message;
    }

    List<PackedContainer> containers() { return containers; }
    boolean failed() { return failed; }
    String message() { return message; }
    boolean isMultiContainer() { return containers.size() > 1; }
}

final class GroupItemBucket {
    private final List<SchedulableItem> items;

    GroupItemBucket(List<SchedulableItem> items) {
        this.items = new ArrayList<SchedulableItem>(items);
    }

    List<SchedulableItem> items() { return items; }
    boolean isEmpty() { return items.isEmpty(); }
    void removeAll(List<SchedulableItem> removed) { items.removeAll(removed); }

    List<SchedulableItem> sortedByVolumeDescending() {
        List<SchedulableItem> sorted = new ArrayList<SchedulableItem>(items);
        Collections.sort(sorted, new Comparator<SchedulableItem>() {
            @Override
            public int compare(SchedulableItem first, SchedulableItem second) {
                int volumeCompare = Long.compare(second.volume(), first.volume());
                if (volumeCompare != 0) {
                    return volumeCompare;
                }
                return first.runtimeId().compareTo(second.runtimeId());
            }
        });
        return sorted;
    }
}

class BusinessException extends RuntimeException {
    BusinessException(String message) {
        super(message);
    }
}
