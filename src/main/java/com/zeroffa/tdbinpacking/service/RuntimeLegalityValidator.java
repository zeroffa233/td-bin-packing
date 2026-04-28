package com.zeroffa.tdbinpacking.service;

import java.math.BigDecimal;

class RuntimeLegalityValidator {

    boolean isLegal(SingleBoxPackingAttempt attempt) {
        return isWeightLegal(attempt) && isCapacityLegal(attempt);
    }

    private boolean isWeightLegal(SingleBoxPackingAttempt attempt) {
        BigDecimal maxWeight = attempt.container().maxWeight();
        if (maxWeight == null) {
            return true;
        }
        BigDecimal totalWeight = attempt.packedItems().stream()
                .map(PackedItem::item)
                .map(SchedulableItem::totalWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalWeight.compareTo(maxWeight) <= 0;
    }

    private boolean isCapacityLegal(SingleBoxPackingAttempt attempt) {
        BigDecimal capacityRate = attempt.container().capacityRate();
        if (capacityRate == null) {
            return true;
        }
        BigDecimal utilization = BigDecimal.valueOf(attempt.utilization());
        if (capacityRate.compareTo(BigDecimal.ONE) > 0) {
            utilization = utilization.multiply(BigDecimal.valueOf(100L));
        }
        return utilization.compareTo(capacityRate) <= 0;
    }
}
