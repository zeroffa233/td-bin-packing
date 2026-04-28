package com.zeroffa.tdbinpacking.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PackingJsonCodec {

    public PackingRequest fromJson(String json) {
        Object parsed = new SimpleJsonParser(json).parse();
        Map<String, Object> root = object(parsed, "root");
        return new PackingRequest(
                items(list(root.get("items"), "items")),
                containers(list(root.get("containers"), "containers")),
                orderList(root),
                rules(listOrEmpty(root.get("lclRules"))),
                intValue(root.get("unpackJudge"), "unpackJudge", 0)
        );
    }

    public String toJson(PackingResponse response) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"code\": ").append(response.code()).append(",\n");
        builder.append("  \"msg\": ").append(string(response.msg())).append(",\n");
        builder.append("  \"containerCount\": ").append(response.containerCount()).append(",\n");
        builder.append("  \"containerUtilizations\": ");
        appendDoubleArray(builder, response.containerUtilizations());
        builder.append(",\n");
        builder.append("  \"averageUtilization\": ").append(formatDouble(response.averageUtilization())).append(",\n");
        builder.append("  \"data\": {\n");
        builder.append("    \"infos\": [\n");
        List<ResponseInfo> infos = response.data() == null ? Collections.<ResponseInfo>emptyList() : response.data().infos();
        for (int index = 0; index < infos.size(); index++) {
            ResponseInfo info = infos.get(index);
            builder.append("      {\n");
            field(builder, "shipmentDetailId", info.shipmentDetailId(), true, 8);
            field(builder, "shipmentCode", info.shipmentCode(), true, 8);
            field(builder, "waveId", info.waveId(), true, 8);
            field(builder, "itemCode", info.itemCode(), true, 8);
            field(builder, "itemName", info.itemName(), true, 8);
            field(builder, "quantity", info.quantity(), true, 8);
            field(builder, "batch", info.batch(), true, 8);
            field(builder, "lot", info.lot(), true, 8);
            field(builder, "manufactureDate", info.manufactureDate(), true, 8);
            field(builder, "buildCode", info.buildCode(), true, 8);
            field(builder, "zoneCode", info.zoneCode(), true, 8);
            field(builder, "stores", info.stores(), true, 8);
            field(builder, "containerSequence", info.containerSequence(), true, 8);
            field(builder, "containerCode", info.containerCode(), false, 8);
            builder.append("      }").append(index + 1 < infos.size() ? "," : "").append("\n");
        }
        builder.append("    ]\n");
        builder.append("  }\n");
        builder.append("}");
        return builder.toString();
    }

    private void appendDoubleArray(StringBuilder builder, List<Double> values) {
        builder.append("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(formatDouble(values.get(index)));
        }
        builder.append("]");
    }

    private String formatDouble(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private List<RequestItem> items(List<Object> values) {
        List<RequestItem> result = new ArrayList<RequestItem>();
        for (Object value : values) {
            Map<String, Object> item = object(value, "items[]");
            result.add(new RequestItem(
                    text(item.get("code")),
                    text(item.get("itemCategory1")),
                    text(item.get("itemCategory2")),
                    decimal(item.get("weight")),
                    requiredDecimal(item.get("length"), "item.length"),
                    requiredDecimal(item.get("width"), "item.width"),
                    requiredDecimal(item.get("height"), "item.height"),
                    nullableLong(item.get("csQty")),
                    intValue(item.get("allowDown"), "allowDown", 1),
                    decimal(item.get("volume")),
                    text(item.get("name")),
                    text(item.get("brand")),
                    text(item.get("itemSize")),
                    text(item.get("itemColor")),
                    text(item.get("itemStyle"))
            ));
        }
        return result;
    }

    private List<RequestContainer> containers(List<Object> values) {
        List<RequestContainer> result = new ArrayList<RequestContainer>();
        for (Object value : values) {
            Map<String, Object> container = object(value, "containers[]");
            result.add(new RequestContainer(
                    text(container.get("code")),
                    text(container.get("name")),
                    requiredDecimal(container.get("length"), "container.length"),
                    requiredDecimal(container.get("width"), "container.width"),
                    requiredDecimal(container.get("height"), "container.height"),
                    decimal(container.get("maxWeight")),
                    decimal(container.get("capacityRate")),
                    intValue(container.get("sequence"), "container.sequence", 0),
                    longValue(container.get("quantity"), "container.quantity", 0L),
                    text(container.get("stores"))
            ));
        }
        return result;
    }

    private List<OrderDetail> orders(List<Object> values) {
        List<OrderDetail> result = new ArrayList<OrderDetail>();
        for (Object value : values) {
            Map<String, Object> order = object(value, "orders[]");
            result.add(new OrderDetail(
                    longValue(order.get("shipmentDetailId"), "shipmentDetailId", 0L),
                    text(order.get("shipmentCode")),
                    nullableLong(order.get("waveId")),
                    text(order.get("itemCode")),
                    text(order.get("itemName")),
                    longValue(order.get("quantity"), "quantity", 0L),
                    text(order.get("batch")),
                    text(order.get("lot")),
                    text(order.get("manufactureDate")),
                    text(order.get("buildCode")),
                    text(order.get("zoneCode")),
                    text(order.get("stores")),
                    intValue(order.get("carrierService"), "carrierService", 0),
                    intValue(order.get("isTaoX"), "isTaoX", 0),
                    intValue(order.get("groupId"), "groupId", 0)
            ));
        }
        return result;
    }

    private List<OrderDetail> orderList(Map<String, Object> root) {
        Object value = root.containsKey("orders") ? root.get("orders") : root.get("order");
        return orders(list(value, "orders"));
    }

    private List<LclRule> rules(List<Object> values) {
        List<LclRule> result = new ArrayList<LclRule>();
        for (Object value : values) {
            Map<String, Object> rule = object(value, "lclRules[]");
            result.add(new LclRule(
                    intValue(rule.get("sequence"), "lclRule.sequence", 0),
                    text(rule.get("code")),
                    text(rule.get("name"))
            ));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> object(Object value, String fieldName) {
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException(fieldName + " must be an object");
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Object> list(Object value, String fieldName) {
        if (!(value instanceof List)) {
            throw new IllegalArgumentException(fieldName + " must be an array");
        }
        return (List<Object>) value;
    }

    private List<Object> listOrEmpty(Object value) {
        return value == null ? Collections.<Object>emptyList() : list(value, "lclRules");
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal requiredDecimal(Object value, String fieldName) {
        BigDecimal decimal = decimal(value);
        if (decimal == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return decimal;
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private Long nullableLong(Object value) {
        return value == null ? null : Long.valueOf(decimal(value).longValue());
    }

    private long longValue(Object value, String fieldName, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return decimal(value).longValue();
    }

    private int intValue(Object value, String fieldName, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return decimal(value).intValue();
    }

    private void field(StringBuilder builder, String name, Object value, boolean comma, int indent) {
        for (int index = 0; index < indent; index++) {
            builder.append(' ');
        }
        builder.append(string(name)).append(": ");
        if (value == null) {
            builder.append("null");
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else {
            builder.append(string(String.valueOf(value)));
        }
        builder.append(comma ? "," : "").append("\n");
    }

    private String string(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder();
        builder.append('"');
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            switch (ch) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
            }
        }
        builder.append('"');
        return builder.toString();
    }
}
