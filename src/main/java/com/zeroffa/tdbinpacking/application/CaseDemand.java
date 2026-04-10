package com.zeroffa.tdbinpacking.application;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class CaseDemand {

    private final String caseId;
    private final Map<String, Integer> itemQuantities;

    public CaseDemand(String caseId, Map<String, Integer> itemQuantities) {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId must not be blank");
        }
        if (itemQuantities == null || itemQuantities.isEmpty()) {
            throw new IllegalArgumentException("itemQuantities must not be empty");
        }
        this.caseId = caseId;
        this.itemQuantities = Collections.unmodifiableMap(new LinkedHashMap<>(itemQuantities));
    }

    public String getCaseId() {
        return caseId;
    }

    public Map<String, Integer> getItemQuantities() {
        return itemQuantities;
    }
}
