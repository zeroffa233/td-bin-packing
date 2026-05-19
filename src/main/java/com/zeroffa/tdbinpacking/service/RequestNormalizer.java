package com.zeroffa.tdbinpacking.service;

import com.zeroffa.tdbinpacking.api.LclRule;
import com.zeroffa.tdbinpacking.api.OrderDetail;
import com.zeroffa.tdbinpacking.api.PackingRequest;
import com.zeroffa.tdbinpacking.api.RequestContainer;
import com.zeroffa.tdbinpacking.api.RequestItem;
import com.zeroffa.tdbinpacking.api.ResponseInfo;

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
        Map<ItemKey, RequestItem> itemsByCode = buildItemIndex(request.items());
        List<ContainerCandidate> containers = normalizeContainers(request.containers());

        int carrierService = request.order().get(0).carrierService();
        validateCarrierService(carrierService);

        Map<Integer, List<SchedulableItem>> itemsByGroup = new LinkedHashMap<>();
        List<ResponseInfo> preAssignedInfos = new ArrayList<ResponseInfo>();
        int nextPreAssignedContainerSequence = 1;
        for (OrderDetail detail : request.order()) {
            validateUnitCode(detail.unitCode(), "order.unitCode");
            validateCarrierService(detail.carrierService());
            if (detail.carrierService() != carrierService) {
                throw new BusinessException("all orderDetail.carrierService values must be equal");
            }
            RequestItem item = itemsByCode.get(ItemKey.of(detail.itemCode(), detail.unitCode()));
            if (item == null) {
                throw new BusinessException("item not found for itemCode=" + detail.itemCode()
                        + ", unitCode=" + nullSafe(detail.unitCode()));
            }
            QuantityPlan quantityPlan = quantityPlan(detail, item, carrierService, request.unpackJudge());
            if (quantityPlan.preAssignedQuantity > 0L) {
                long caseQuantity = item.csQty() == null ? quantityPlan.preAssignedQuantity : item.csQty().longValue();
                long caseCount = caseQuantity == 0L ? 0L : quantityPlan.preAssignedQuantity / caseQuantity;
                for (long caseIndex = 0L; caseIndex < caseCount; caseIndex++) {
                    preAssignedInfos.add(toPreAssignedInfo(detail, caseQuantity, nextPreAssignedContainerSequence++));
                }
            }
            long schedulableQuantity = quantityPlan.schedulableQuantity;
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
        return new NormalizedRequest(groups, containers, carrierService, rules, request.unpackJudge(),
                Collections.unmodifiableList(new ArrayList<ResponseInfo>(preAssignedInfos)));
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

    private Map<ItemKey, RequestItem> buildItemIndex(List<RequestItem> items) {
        Map<ItemKey, RequestItem> result = new LinkedHashMap<ItemKey, RequestItem>();
        for (RequestItem item : items) {
            if (item.code() == null || item.code().trim().isEmpty()) {
                throw new BusinessException("item.code must not be blank");
            }
            validateUnitCode(item.unitCode(), "item.unitCode");
            if (item.allowDown() != 0 && item.allowDown() != 1) {
                throw new BusinessException("item.allowDown must be 0 or 1: " + item.code());
            }
            ItemKey key = ItemKey.of(item.code(), item.unitCode());
            if (result.containsKey(key)) {
                throw new BusinessException("duplicate item for itemCode=" + item.code()
                        + ", unitCode=" + nullSafe(item.unitCode()));
            }
            result.put(key, item);
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

    private QuantityPlan quantityPlan(OrderDetail detail, RequestItem item, int carrierService, int unpackJudge) {
        long quantity = detail.quantity();
        if (quantity < 0) {
            throw new BusinessException("order.quantity must not be negative");
        }
        if (carrierService != 0) {
            return new QuantityPlan(quantity, 0L);
        }
        if (unpackJudge == 0) {
            return new QuantityPlan(quantity, 0L);
        }
        Long csQty = item.csQty();
        if (csQty == null) {
            return new QuantityPlan(quantity, 0L);
        }
        if (csQty.longValue() == 0L) {
            return new QuantityPlan(quantity, 0L);
        }
        if (csQty.longValue() < 0L) {
            throw new BusinessException("item.csQty must be positive when present");
        }
        long schedulableQuantity = quantity % csQty.longValue();
        long preAssignedQuantity = quantity - schedulableQuantity;
        return new QuantityPlan(schedulableQuantity, preAssignedQuantity);
    }

    private ResponseInfo toPreAssignedInfo(OrderDetail detail, long quantity, int containerSequence) {
        return new ResponseInfo(
                detail.shipmentDetailId(),
                detail.shipmentCode(),
                detail.waveId(),
                detail.itemCode(),
                detail.itemName(),
                quantity,
                detail.batch(),
                detail.lot(),
                detail.manufactureDate(),
                detail.buildCode(),
                detail.zoneCode(),
                detail.stores(),
                Integer.valueOf(containerSequence),
                "CS"
        );
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

    private void validateUnitCode(String unitCode, String field) {
        if (unitCode == null || unitCode.trim().isEmpty()) {
            return;
        }
        String normalized = unitCode.trim();
        if (!"CS".equals(normalized) && !"EA".equals(normalized) && !"MP".equals(normalized)
                && !"PL".equals(normalized) && !"SP".equals(normalized)) {
            throw new BusinessException(field + " must be one of CS, EA, MP, PL, SP");
        }
    }

    private String nullSafe(String value) {
        return value == null ? "null" : value;
    }

    private String resolveGroupStores(List<SchedulableItem> items) {
        return items.stream()
                .map(SchedulableItem::stores)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static final class QuantityPlan {
        private final long schedulableQuantity;
        private final long preAssignedQuantity;

        private QuantityPlan(long schedulableQuantity, long preAssignedQuantity) {
            this.schedulableQuantity = schedulableQuantity;
            this.preAssignedQuantity = preAssignedQuantity;
        }
    }

    private static final class ItemKey {
        private final String itemCode;
        private final String unitCode;

        private ItemKey(String itemCode, String unitCode) {
            this.itemCode = itemCode;
            this.unitCode = unitCode;
        }

        private static ItemKey of(String itemCode, String unitCode) {
            return new ItemKey(itemCode, unitCode);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ItemKey)) {
                return false;
            }
            ItemKey that = (ItemKey) other;
            return Objects.equals(itemCode, that.itemCode) && Objects.equals(unitCode, that.unitCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(itemCode, unitCode);
        }
    }
}
