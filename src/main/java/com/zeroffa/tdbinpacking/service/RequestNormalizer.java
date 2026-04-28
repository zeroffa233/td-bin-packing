package com.zeroffa.tdbinpacking.service;

import com.zeroffa.tdbinpacking.api.LclRule;
import com.zeroffa.tdbinpacking.api.OrderDetail;
import com.zeroffa.tdbinpacking.api.PackingRequest;
import com.zeroffa.tdbinpacking.api.RequestContainer;
import com.zeroffa.tdbinpacking.api.RequestItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RequestNormalizer {

    private final ScaleConfig scaleConfig;

    public RequestNormalizer() {
        this(ScaleConfig.defaultConfig());
    }

    RequestNormalizer(ScaleConfig scaleConfig) {
        this.scaleConfig = Objects.requireNonNull(scaleConfig, "scaleConfig");
    }

    NormalizedRequest normalize(PackingRequest request) {
        validateRequest(request);
        Map<String, RequestItem> itemsByCode = buildItemIndex(request.items());
        List<ContainerCandidate> containers = normalizeContainers(request.containers());

        int carrierService = request.order().get(0).carrierService();
        validateCarrierService(carrierService);

        Map<Integer, List<SchedulableItem>> itemsByGroup = new LinkedHashMap<>();
        for (OrderDetail detail : request.order()) {
            validateCarrierService(detail.carrierService());
            if (detail.carrierService() != carrierService) {
                throw new BusinessException("all orderDetail.carrierService values must be equal");
            }
            RequestItem item = itemsByCode.get(detail.itemCode());
            if (item == null) {
                throw new BusinessException("itemCode not found: " + detail.itemCode());
            }
            long schedulableQuantity = schedulableQuantity(detail.quantity(), item.csQty());
            if (schedulableQuantity == 0L) {
                continue;
            }
            int sizeX = scaleDimension(item.length(), "item.length");
            int sizeY = scaleDimension(item.width(), "item.width");
            int sizeZ = scaleDimension(item.height(), "item.height");
            for (long unitIndex = 0; unitIndex < schedulableQuantity; unitIndex++) {
                String runtimeId = detail.shipmentDetailId() + ":" + detail.itemCode() + ":" + unitIndex;
                SchedulableItem schedulableItem = SchedulableItem.unit(runtimeId, unitIndex, item, detail,
                        sizeX, sizeY, sizeZ);
                itemsByGroup.computeIfAbsent(detail.groupId(), ignored -> new ArrayList<>()).add(schedulableItem);
            }
        }

        Map<Integer, PackingGroup> groups = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<SchedulableItem>> entry : itemsByGroup.entrySet()) {
            List<SchedulableItem> groupItems = Collections.unmodifiableList(new ArrayList<SchedulableItem>(entry.getValue()));
            groups.put(entry.getKey(), new PackingGroup(entry.getKey(), groupItems, resolveGroupStores(groupItems)));
        }

        List<LclRuleSpec> rules = normalizeRules(request.lclRules());
        return new NormalizedRequest(groups, containers, carrierService, rules, request.unpackJudge());
    }

    private void validateRequest(PackingRequest request) {
        if (request == null) {
            throw new BusinessException("request must not be null");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new BusinessException("request.items must not be empty");
        }
        if (request.containers() == null || request.containers().isEmpty()) {
            throw new BusinessException("request.containers must not be empty");
        }
        if (request.order() == null || request.order().isEmpty()) {
            throw new BusinessException("request.order must not be empty");
        }
        if (request.unpackJudge() != 0 && request.unpackJudge() != 1) {
            throw new BusinessException("unpackJudge must be 0 or 1");
        }
    }

    private Map<String, RequestItem> buildItemIndex(List<RequestItem> items) {
        Map<String, RequestItem> result = new LinkedHashMap<>();
        for (RequestItem item : items) {
            if (item.code() == null || item.code().trim().isEmpty()) {
                throw new BusinessException("item.code must not be blank");
            }
            if (item.allowDown() != 0 && item.allowDown() != 1) {
                throw new BusinessException("item.allowDown must be 0 or 1: " + item.code());
            }
            result.put(item.code(), item);
        }
        return result;
    }

    private List<ContainerCandidate> normalizeContainers(List<RequestContainer> containers) {
        List<ContainerCandidate> result = new ArrayList<>();
        for (RequestContainer container : containers) {
            if (container.code() == null || container.code().trim().isEmpty()) {
                throw new BusinessException("container.code must not be blank");
            }
            if (container.quantity() < 0) {
                throw new BusinessException("container.quantity must not be negative: " + container.code());
            }
            int sizeX = scaleDimension(container.length(), "container.length");
            int sizeY = scaleDimension(container.width(), "container.width");
            int sizeZ = scaleDimension(container.height(), "container.height");
            result.add(ContainerCandidate.from(container, sizeX, sizeY, sizeZ));
        }
        return result;
    }

    private List<LclRuleSpec> normalizeRules(List<LclRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return Collections.singletonList(new LclRuleSpec(0, RuleCode.NO_LIMIT, "NO_LIMIT"));
        }
        return rules.stream()
                .map(LclRuleSpec::from)
                .sorted(Comparator.comparingInt(LclRuleSpec::sequence))
                .collect(Collectors.toList());
    }

    private long schedulableQuantity(long quantity, Long csQty) {
        if (quantity < 0) {
            throw new BusinessException("order.quantity must not be negative");
        }
        if (csQty == null) {
            return quantity;
        }
        if (csQty <= 0) {
            throw new BusinessException("item.csQty must be positive when present");
        }
        return quantity % csQty;
    }

    private int scaleDimension(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new BusinessException(field + " must be positive");
        }
        BigDecimal scaled = value.multiply(BigDecimal.valueOf(scaleConfig.dimensionScale()))
                .setScale(0, RoundingMode.HALF_UP);
        if (scaled.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new BusinessException(field + " is too large after scaling");
        }
        int intValue = scaled.intValueExact();
        if (intValue <= 0) {
            throw new BusinessException(field + " must be positive after scaling");
        }
        return intValue;
    }

    private void validateCarrierService(int carrierService) {
        if (carrierService != 0 && carrierService != 1) {
            throw new BusinessException("carrierService must be 0 or 1");
        }
    }

    private String resolveGroupStores(List<SchedulableItem> items) {
        return items.stream()
                .map(SchedulableItem::stores)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
