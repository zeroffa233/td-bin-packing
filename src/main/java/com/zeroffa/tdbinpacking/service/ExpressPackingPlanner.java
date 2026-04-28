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
                    "no container matches stores for group " + group.groupId());
        }
        return lclRuleOrchestrator.pack(group.groupId(), preprocessedItems, candidates, rules);
    }
}
