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

    PackingResponse assemble(RequestPackingPlan plan, int unpackJudge) {
        int code = plan.failed() ? 1 : 0;
        int ifMultiContainers = plan.isMultiContainer() ? 1 : 0;
        if (code == 0 && ifMultiContainers == 1 && unpackJudge == 0) {
            code = 1;
        }

        String msg = code == 0 ? "OK" : defaultMessage(plan.message());
        List<Double> utilizations = containerUtilizations(plan.containers());
        return new PackingResponse(
                code,
                msg,
                new ResponseData(expandInfos(plan.containers())),
                plan.containers().size(),
                Collections.unmodifiableList(utilizations),
                average(utilizations)
        );
    }

    private List<Double> containerUtilizations(List<PackedContainer> containers) {
        List<Double> utilizations = new ArrayList<>();
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

    private List<ResponseInfo> expandInfos(List<PackedContainer> containers) {
        List<ResponseInfo> result = new ArrayList<>();
        int containerSequence = 1;
        for (PackedContainer container : containers) {
            Map<InfoKey, Long> quantities = new LinkedHashMap<>();
            for (PackedItem packedItem : container.packedItems()) {
                for (SchedulableItem represented : packedItem.item().representedItems()) {
                    quantities.merge(InfoKey.from(represented.source()), 1L, Long::sum);
                }
            }
            for (Map.Entry<InfoKey, Long> entry : quantities.entrySet()) {
                result.add(entry.getKey().toResponseInfo(entry.getValue(), containerSequence,
                        container.container().code()));
            }
            containerSequence++;
        }
        return result;
    }

    private String defaultMessage(String message) {
        return message == null || message.trim().isEmpty() ? "packing failed" : message;
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
