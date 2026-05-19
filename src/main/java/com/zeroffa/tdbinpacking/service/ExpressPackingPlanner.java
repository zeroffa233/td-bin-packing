package com.zeroffa.tdbinpacking.service;

import java.util.Collections;
import java.util.List;

class ExpressPackingPlanner {

    private final ItemPreprocessor itemPreprocessor;
    private final ContainerCandidateService containerCandidateService;
    private final LclRuleOrchestrator lclRuleOrchestrator;

    ExpressPackingPlanner(ItemPreprocessor itemPreprocessor,
                          ContainerCandidateService containerCandidateService,
                          LclRuleOrchestrator lclRuleOrchestrator) {
        this.itemPreprocessor = itemPreprocessor;
        this.containerCandidateService = containerCandidateService;
        this.lclRuleOrchestrator = lclRuleOrchestrator;
    }

    GroupPackingPlan pack(PackingGroup group, List<ContainerCandidate> containers, List<LclRuleSpec> rules) {
        List<SchedulableItem> preprocessedItems = itemPreprocessor.preprocess(group);
        if (preprocessedItems.isEmpty()) {
            return new GroupPackingPlan(group.groupId(), Collections.<PackedContainer>emptyList(), false, "");
        }
        List<ContainerCandidate> candidates = containerCandidateService.candidatesForGroup(group, containers);
        if (candidates.isEmpty()) {
            return new GroupPackingPlan(group.groupId(), Collections.<PackedContainer>emptyList(), true,
                    "group " + group.groupId() + " 装箱失败：未找到匹配门店[" + safe(group.stores()) + "]的可用箱型");
        }
        return lclRuleOrchestrator.pack(group.groupId(), preprocessedItems, candidates, rules);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "未指定" : value;
    }
}
