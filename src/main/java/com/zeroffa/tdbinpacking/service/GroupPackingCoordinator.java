package com.zeroffa.tdbinpacking.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class GroupPackingCoordinator {

    private final ExpressPackingPlanner expressPackingPlanner;
    private final LogisticShortcutPlanner logisticShortcutPlanner;

    GroupPackingCoordinator(ExpressPackingPlanner expressPackingPlanner,
                            LogisticShortcutPlanner logisticShortcutPlanner) {
        this.expressPackingPlanner = expressPackingPlanner;
        this.logisticShortcutPlanner = logisticShortcutPlanner;
    }

    RequestPackingPlan pack(NormalizedRequest request) {
        List<PackedContainer> allContainers = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        boolean failed = false;

        for (PackingGroup group : request.groups().values()) {
            GroupPackingPlan groupPlan = request.carrierService() == 1
                    ? logisticShortcutPlanner.pack(group, request.containers())
                    : expressPackingPlanner.pack(group, request.containers(), request.lclRules());
            allContainers.addAll(groupPlan.containers());
            if (groupPlan.failed()) {
                failed = true;
                if (groupPlan.message() != null && !groupPlan.message().trim().isEmpty()) {
                    messages.add(groupPlan.message());
                }
            }
        }

        return new RequestPackingPlan(Collections.unmodifiableList(new ArrayList<PackedContainer>(allContainers)), failed, String.join("; ", messages));
    }
}
