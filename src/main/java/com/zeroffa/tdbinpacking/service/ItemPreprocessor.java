package com.zeroffa.tdbinpacking.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ItemPreprocessor {

    List<SchedulableItem> preprocess(PackingGroup group) {
        List<SchedulableItem> lhItems = new ArrayList<>();
        List<SchedulableItem> positives = new ArrayList<>();
        List<SchedulableItem> samplesAndGifts = new ArrayList<>();
        List<SchedulableItem> directItems = new ArrayList<>();

        for (SchedulableItem item : group.items()) {
            if (isCategory(item, "LH")) {
                lhItems.add(item);
            } else if (isCategory(item, "正品")) {
                positives.add(item);
            } else if (isCategory(item, "小样") || isCategory(item, "赠品")) {
                samplesAndGifts.add(item);
            } else {
                directItems.add(item);
            }
        }

        if (lhItems.isEmpty()) {
            return Collections.unmodifiableList(new ArrayList<SchedulableItem>(group.items()));
        }

        List<List<SchedulableItem>> representedByLh = new ArrayList<>();
        for (SchedulableItem lhItem : lhItems) {
            representedByLh.add(new ArrayList<SchedulableItem>(java.util.Collections.singletonList(lhItem)));
        }

        distributeRoundRobin(positives, representedByLh);
        distributeRoundRobin(samplesAndGifts, representedByLh);

        List<SchedulableItem> result = new ArrayList<>();
        for (int index = 0; index < lhItems.size(); index++) {
            result.add(lhItems.get(index).withRepresentedItems(representedByLh.get(index)));
        }
        result.addAll(directItems);
        return result;
    }

    private void distributeRoundRobin(List<SchedulableItem> items, List<List<SchedulableItem>> targets) {
        for (int index = 0; index < items.size(); index++) {
            targets.get(index % targets.size()).add(items.get(index));
        }
    }

    private boolean isCategory(SchedulableItem item, String category) {
        return category.equalsIgnoreCase(nullToEmpty(item.itemCategory1()));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
