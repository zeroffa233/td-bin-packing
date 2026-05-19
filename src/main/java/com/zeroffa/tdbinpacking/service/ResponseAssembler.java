package com.zeroffa.tdbinpacking.service;

import com.zeroffa.tdbinpacking.api.OrderDetail;
import com.zeroffa.tdbinpacking.api.PackingResponse;
import com.zeroffa.tdbinpacking.api.ResponseData;
import com.zeroffa.tdbinpacking.api.ResponseInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class ResponseAssembler {

    PackingResponse assemble(RequestPackingPlan plan, int unpackJudge, List<ResponseInfo> preAssignedInfos) {
        int code = plan.failed() ? 1 : 0;
        int ifMultiContainers = plan.isMultiContainer() ? 1 : 0;
        boolean singleContainerRequiredButSplit = code == 0 && ifMultiContainers == 1 && unpackJudge == 0;
        if (singleContainerRequiredButSplit) {
            code = 1;
        }

        String msg = code == 0 ? "OK" : buildFailureMessage(plan, singleContainerRequiredButSplit);
        int preAssignedContainerCount = preAssignedContainerCount(preAssignedInfos);
        List<Double> utilizations = containerUtilizations(plan.containers(), preAssignedContainerCount);
        List<ResponseInfo> infos = new ArrayList<ResponseInfo>();
        if (preAssignedInfos != null && !preAssignedInfos.isEmpty()) {
            infos.addAll(preAssignedInfos);
        }
        infos.addAll(expandInfos(plan.containers(), preAssignedContainerCount + 1));
        return new PackingResponse(
                code,
                msg,
                new ResponseData(infos),
                preAssignedContainerCount + plan.containers().size(),
                Collections.unmodifiableList(utilizations),
                average(utilizations)
        );
    }

    private String buildFailureMessage(RequestPackingPlan plan, boolean singleContainerRequiredButSplit) {
        if (singleContainerRequiredButSplit) {
            String suggestion = formatContainerSuggestions(plan.containers());
            return suggestion.isEmpty()
                    ? "未找到可容纳的单箱箱型，且未生成可参考的拆箱方案"
                    : "未找到可容纳的单箱箱型，建议拆箱：" + suggestion;
        }
        return defaultMessage(plan.message());
    }

    private List<Double> containerUtilizations(List<PackedContainer> containers, int preAssignedContainerCount) {
        List<Double> utilizations = new ArrayList<Double>();
        for (int index = 0; index < preAssignedContainerCount; index++) {
            utilizations.add(1.0D);
        }
        for (PackedContainer container : containers) {
            long packedVolume = 0L;
            for (PackedItem packedItem : container.packedItems()) {
                packedVolume += packedItem.item().volume();
            }
            long containerVolume = container.container().volume();
            utilizations.add(containerVolume == 0L ? 0.0D : (double) packedVolume / containerVolume);
        }
        return utilizations;
    }

    private double average(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0D;
        }
        double total = 0.0D;
        for (Double value : values) {
            total += value;
        }
        return total / values.size();
    }

    private List<ResponseInfo> expandInfos(List<PackedContainer> containers, int startingContainerSequence) {
        List<ResponseInfo> result = new ArrayList<ResponseInfo>();
        int containerSequence = startingContainerSequence;
        for (PackedContainer container : containers) {
            Map<InfoKey, Long> quantities = new LinkedHashMap<InfoKey, Long>();
            for (PackedItem packedItem : container.packedItems()) {
                for (SchedulableItem represented : packedItem.item().representedItems()) {
                    Long current = quantities.get(InfoKey.from(represented.source()));
                    quantities.put(InfoKey.from(represented.source()), current == null ? 1L : current + 1L);
                }
            }
            for (Map.Entry<InfoKey, Long> entry : quantities.entrySet()) {
                result.add(entry.getKey().toResponseInfo(entry.getValue().longValue(), containerSequence,
                        container.container().code()));
            }
            containerSequence++;
        }
        return result;
    }

    private int preAssignedContainerCount(List<ResponseInfo> preAssignedInfos) {
        int count = 0;
        if (preAssignedInfos == null) {
            return count;
        }
        for (ResponseInfo info : preAssignedInfos) {
            if (info.containerSequence() != null && info.containerSequence().intValue() > count) {
                count = info.containerSequence().intValue();
            }
        }
        return count;
    }

    private String formatContainerSuggestions(List<PackedContainer> containers) {
        List<String> parts = new ArrayList<String>();
        for (PackedContainer container : containers) {
            String containerItems = formatContainerItems(container);
            if (!containerItems.isEmpty()) {
                parts.add("{" + containerItems + "}");
            }
        }
        return join(parts, "; ");
    }

    private String formatContainerItems(PackedContainer container) {
        Map<String, Long> quantities = new LinkedHashMap<String, Long>();
        for (PackedItem packedItem : container.packedItems()) {
            for (SchedulableItem represented : packedItem.item().representedItems()) {
                String itemCode = represented.itemCode();
                Long current = quantities.get(itemCode);
                quantities.put(itemCode, current == null ? 1L : current + 1L);
            }
        }
        List<String> parts = new ArrayList<String>();
        for (Map.Entry<String, Long> entry : quantities.entrySet()) {
            parts.add(entry.getKey() + "*" + entry.getValue());
        }
        return join(parts, ",");
    }

    private String join(List<String> values, String separator) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(separator);
            }
            builder.append(values.get(index));
        }
        return builder.toString();
    }

    private String defaultMessage(String message) {
        return message == null || message.trim().isEmpty() ? "装箱失败：未得到可执行装箱结果" : message;
    }

    private static class InfoKey {
        private final OrderDetail source;

        private InfoKey(OrderDetail source) {
            this.source = source;
        }

        static InfoKey from(OrderDetail source) {
            return new InfoKey(source);
        }

        ResponseInfo toResponseInfo(long quantity, int containerSequence, String containerCode) {
            return new ResponseInfo(
                    source.shipmentDetailId(),
                    source.shipmentCode(),
                    source.waveId(),
                    source.itemCode(),
                    source.itemName(),
                    quantity,
                    source.batch(),
                    source.lot(),
                    source.manufactureDate(),
                    source.buildCode(),
                    source.zoneCode(),
                    source.stores(),
                    containerSequence,
                    containerCode
            );
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof InfoKey)) {
                return false;
            }
            InfoKey that = (InfoKey) other;
            return source == that.source;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(source);
        }
    }
}
