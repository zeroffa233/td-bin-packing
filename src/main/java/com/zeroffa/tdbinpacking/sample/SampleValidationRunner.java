package com.zeroffa.tdbinpacking.sample;

import com.zeroffa.tdbinpacking.api.LclRule;
import com.zeroffa.tdbinpacking.api.OrderDetail;
import com.zeroffa.tdbinpacking.api.PackingRequest;
import com.zeroffa.tdbinpacking.api.PackingResponse;
import com.zeroffa.tdbinpacking.api.RequestContainer;
import com.zeroffa.tdbinpacking.api.RequestItem;
import com.zeroffa.tdbinpacking.api.ResponseInfo;
import com.zeroffa.tdbinpacking.service.PackingService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SampleValidationRunner {

    public static void main(String[] args) {
        PackingResponse response = new PackingService().pack(sampleOneRequest());
        assertSuccess(response);
        printResponse(response);
    }

    private static PackingRequest sampleOneRequest() {
        List<RequestItem> items = Collections.singletonList(new RequestItem(
                "G4410201",
                "正品",
                "非易碎",
                decimal("0.508"),
                decimal("0.215"),
                decimal("0.121"),
                decimal("0.067"),
                8L,
                1,
                decimal("0.001743005"),
                "货品-G4410201",
                "巴欧",
                "",
                "",
                "STYLE-G4410201"
        ));

        List<RequestContainer> containers = Arrays.asList(
                container("LRLC0002W", "巴欧C2物流箱", "0.18", "0.11", "0.09", 1),
                container("LRLC0003W", "巴欧C3物流箱", "0.21", "0.12", "0.115", 2),
                container("LRLC0004W", "巴欧C4物流箱", "0.24", "0.15", "0.12", 3),
                container("LRLC0005W", "巴欧C5物流箱", "0.24", "0.19", "0.13", 4),
                container("LRLC0010H", "巴欧C10物流箱", "0.427", "0.325", "0.328", 5),
                container("COM00002L", "通用物流中箱", "0.4", "0.306", "0.506", 6)
        );

        List<OrderDetail> orders = Collections.singletonList(new OrderDetail(
                2026041601001L,
                "TSHD2026041200015736",
                null,
                "G4410201",
                "货品-G4410201",
                1L,
                null,
                null,
                null,
                "BUILD-BAO-01",
                "ZONE-BAO-01",
                "巴欧",
                0,
                0,
                0
        ));

        return new PackingRequest(
                items,
                containers,
                orders,
                Collections.singletonList(new LclRule(7, "NO_LIMIT", "不限制")),
                0
        );
    }

    private static RequestContainer container(String code, String name, String length, String width, String height, int sequence) {
        return new RequestContainer(
                code,
                name,
                decimal(length),
                decimal(width),
                decimal(height),
                null,
                decimal("97"),
                sequence,
                999L,
                "巴欧"
        );
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private static void assertSuccess(PackingResponse response) {
        if (response.code() != 0) {
            throw new IllegalStateException("sample one failed: " + response.msg());
        }
        if (response.data() == null || response.data().infos() == null || response.data().infos().size() != 1) {
            throw new IllegalStateException("sample one should produce exactly one info row");
        }
        ResponseInfo info = response.data().infos().get(0);
        if (!"G4410201".equals(info.itemCode())) {
            throw new IllegalStateException("unexpected itemCode: " + info.itemCode());
        }
        if (info.containerCode() == null || info.containerCode().trim().isEmpty()) {
            throw new IllegalStateException("containerCode should not be blank");
        }
    }

    private static void printResponse(PackingResponse response) {
        System.out.println("{");
        System.out.println("  \"code\": " + response.code() + ",");
        System.out.println("  \"msg\": \"" + response.msg() + "\",");
        System.out.println("  \"data\": {");
        System.out.println("    \"infos\": [");
        for (int index = 0; index < response.data().infos().size(); index++) {
            ResponseInfo info = response.data().infos().get(index);
            System.out.println("      {");
            System.out.println("        \"shipmentDetailId\": " + info.shipmentDetailId() + ",");
            System.out.println("        \"shipmentCode\": \"" + info.shipmentCode() + "\",");
            System.out.println("        \"waveId\": " + info.waveId() + ",");
            System.out.println("        \"itemCode\": \"" + info.itemCode() + "\",");
            System.out.println("        \"itemName\": \"" + info.itemName() + "\",");
            System.out.println("        \"quantity\": " + info.quantity() + ",");
            System.out.println("        \"containerSequence\": " + info.containerSequence() + ",");
            System.out.println("        \"containerCode\": \"" + info.containerCode() + "\"");
            System.out.println("      }" + (index + 1 < response.data().infos().size() ? "," : ""));
        }
        System.out.println("    ]");
        System.out.println("  }");
        System.out.println("}");
    }
}
