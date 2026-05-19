package com.zeroffa.tdbinpacking.service;

import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class LogisticShortcutPlanner {

    private final ContainerCandidateService containerCandidateService;

    LogisticShortcutPlanner(ContainerCandidateService containerCandidateService) {
        this.containerCandidateService = containerCandidateService;
    }

    GroupPackingPlan pack(PackingGroup group, List<ContainerCandidate> containers) {
        return containerCandidateService.candidatesForGroup(group, containers).stream()
                .min(Comparator.comparingLong(ContainerCandidate::volume).thenComparing(ContainerCandidate::code))
                .map(container -> toPlan(group, container))
                .orElseGet(() -> new GroupPackingPlan(group.groupId(), Collections.<PackedContainer>emptyList(), true,
                        "group " + group.groupId() + " 装箱失败：未找到匹配门店[" + safe(group.stores()) + "]的物流捷径箱型"));
    }

    private GroupPackingPlan toPlan(PackingGroup group, ContainerCandidate container) {
        List<PackedItem> packedItems = group.items().stream()
                .map(item -> new PackedItem(item, null))
                .collect(Collectors.toList());
        return new GroupPackingPlan(group.groupId(),
                Collections.singletonList(new PackedContainer(group.groupId(), container, packedItems)),
                false,
                "");
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "未指定" : value;
    }
}
