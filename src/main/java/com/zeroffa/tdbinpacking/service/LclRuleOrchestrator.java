package com.zeroffa.tdbinpacking.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class LclRuleOrchestrator {

    private final CorePackerAdapter corePacker;
    private final RuntimeLegalityValidator legalityValidator;
    private final TraditionalMultiBoxPlanner traditionalPlanner;

    LclRuleOrchestrator(CorePackerAdapter corePacker,
                        RuntimeLegalityValidator legalityValidator,
                        TraditionalMultiBoxPlanner traditionalPlanner) {
        this.corePacker = corePacker;
        this.legalityValidator = legalityValidator;
        this.traditionalPlanner = traditionalPlanner;
    }

    GroupPackingPlan pack(int groupId,
                          List<SchedulableItem> items,
                          List<ContainerCandidate> candidates,
                          List<LclRuleSpec> rules) {
        GroupItemBucket remaining = new GroupItemBucket(items);
        List<PackedContainer> lockedContainers = new ArrayList<>();

        while (!remaining.isEmpty()) {
            RuleCycleResult cycleResult = runRuleCycle(groupId, remaining, candidates, rules);
            lockedContainers.addAll(cycleResult.containers());
            if (cycleResult.failed()) {
                return new GroupPackingPlan(groupId, Collections.unmodifiableList(new ArrayList<PackedContainer>(lockedContainers)), true, cycleResult.message());
            }
        }

        return new GroupPackingPlan(groupId, Collections.unmodifiableList(new ArrayList<PackedContainer>(lockedContainers)), false, "");
    }

    private RuleCycleResult runRuleCycle(int groupId,
                                         GroupItemBucket remaining,
                                         List<ContainerCandidate> candidates,
                                         List<LclRuleSpec> rules) {
        for (int ruleIndex = 0; ruleIndex < rules.size(); ruleIndex++) {
            LclRuleSpec rule = rules.get(ruleIndex);
            List<List<SchedulableItem>> subsets = extractSubsets(rule, remaining.items());
            for (int subsetIndex = 0; subsetIndex < subsets.size(); subsetIndex++) {
                List<SchedulableItem> subset = subsets.get(subsetIndex);
                if (subset.isEmpty()) {
                    continue;
                }
                if (subset.size() == remaining.items().size()) {
                    GroupPackingPlan plan = traditionalPlanner.packAll(groupId, remaining.items(), candidates);
                    removePackedItems(remaining, plan.containers());
                    return new RuleCycleResult(plan.containers(), plan.failed(), plan.message());
                }

                BoundaryResult boundaryResult = findBoundaryAttempt(subset, candidates);
                if (boundaryResult.status() == BoundaryStatus.BOUNDARY_FOUND) {
                    PackedContainer packedContainer = lockBoundary(groupId, boundaryResult.attempt(), remaining);
                    return new RuleCycleResult(Collections.singletonList(packedContainer), false, "");
                }
                if (boundaryResult.status() == BoundaryStatus.TOO_STRICT && subsetIndex + 1 < subsets.size()) {
                    continue;
                }
                break;
            }
        }

        GroupPackingPlan fallback = traditionalPlanner.packAll(groupId, remaining.items(), candidates);
        removePackedItems(remaining, fallback.containers());
        String message = fallback.failed()
                ? "lclRules fallback failed in group " + groupId + ": " + fallback.message()
                : "lclRules fallback used in group " + groupId;
        return new RuleCycleResult(fallback.containers(), true, message);
    }

    private void removePackedItems(GroupItemBucket remaining, List<PackedContainer> containers) {
        List<SchedulableItem> packedItems = new ArrayList<>();
        for (PackedContainer container : containers) {
            for (PackedItem packedItem : container.packedItems()) {
                packedItems.add(packedItem.item());
            }
        }
        remaining.removeAll(packedItems);
    }

    private BoundaryResult findBoundaryAttempt(List<SchedulableItem> subset, List<ContainerCandidate> candidates) {
        BoundaryResult tooStrict = null;
        for (ContainerCandidate candidate : candidates) {
            if (!candidate.hasInventory()) {
                continue;
            }
            SingleBoxPackingAttempt attempt = corePacker.pack(candidate, subset);
            if (attempt.packedItems().isEmpty()) {
                continue;
            }
            if (attempt.allPlaced()) {
                if (legalityValidator.isLegal(attempt)) {
                    tooStrict = new BoundaryResult(BoundaryStatus.TOO_STRICT, attempt);
                    break;
                }
                continue;
            }
            if (legalityValidator.isLegal(attempt)) {
                return new BoundaryResult(BoundaryStatus.BOUNDARY_FOUND, attempt);
            }
        }
        return tooStrict == null ? new BoundaryResult(BoundaryStatus.NO_CANDIDATE, null) : tooStrict;
    }

    private PackedContainer lockBoundary(int groupId, SingleBoxPackingAttempt attempt, GroupItemBucket remaining) {
        PackedContainer packedContainer = new PackedContainer(groupId, attempt.container(), attempt.packedItems());
        attempt.container().decrement();
        remaining.removeAll(attempt.packedItems().stream().map(PackedItem::item).collect(Collectors.toList()));
        return packedContainer;
    }

    private List<List<SchedulableItem>> extractSubsets(LclRuleSpec rule, List<SchedulableItem> items) {
        if (rule.code() == RuleCode.NO_LIMIT) {
            return Collections.singletonList(new ArrayList<SchedulableItem>(items));
        }
        Function<SchedulableItem, String> keyFunction = keyFunction(rule.code());
        Map<String, List<SchedulableItem>> groups = items.stream()
                .collect(Collectors.groupingBy(keyFunction, LinkedHashMap::new, Collectors.toList()));
        return groups.values().stream()
                .filter(group -> !group.isEmpty())
                .sorted(Comparator.comparingInt(List<SchedulableItem>::size)
                        .thenComparing(group -> group.get(0).runtimeId()))
                .map(group -> new ArrayList<SchedulableItem>(group))
                .collect(Collectors.toList());
    }

    private Function<SchedulableItem, String> keyFunction(RuleCode ruleCode) {
        switch (ruleCode) {
            case SAME_SKU_LOT:
                return item -> join(item.itemCode(), item.batch(), item.lot(), item.manufactureDate());
            case SAME_SKU:
                return SchedulableItem::itemCode;
            case SAME_TYPE:
                return item -> nullToEmpty(item.itemStyle());
            case SAME_BUILD_ZONE:
                return item -> join(item.buildCode(), item.zoneCode());
            case SAME_BUILD:
                return item -> nullToEmpty(item.buildCode());
            case SAME_ZONE:
                return item -> nullToEmpty(item.zoneCode());
            case NO_LIMIT:
                return item -> "ALL";
            default:
                throw new IllegalArgumentException("unsupported rule code: " + ruleCode);
        }
    }

    private String join(String... values) {
        return java.util.Arrays.stream(values).map(this::nullToEmpty).collect(Collectors.joining("|"));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private enum BoundaryStatus {
        BOUNDARY_FOUND,
        TOO_STRICT,
        NO_CANDIDATE
    }

    private static class BoundaryResult {
        private final BoundaryStatus status;
        private final SingleBoxPackingAttempt attempt;

        private BoundaryResult(BoundaryStatus status, SingleBoxPackingAttempt attempt) {
            this.status = status;
            this.attempt = attempt;
        }

        private BoundaryStatus status() {
            return status;
        }

        private SingleBoxPackingAttempt attempt() {
            return attempt;
        }
    }

    private static class RuleCycleResult {
        private final List<PackedContainer> containers;
        private final boolean failed;
        private final String message;

        private RuleCycleResult(List<PackedContainer> containers, boolean failed, String message) {
            this.containers = containers;
            this.failed = failed;
            this.message = message;
        }

        private List<PackedContainer> containers() {
            return containers;
        }

        private boolean failed() {
            return failed;
        }

        private String message() {
            return message;
        }
    }
}
