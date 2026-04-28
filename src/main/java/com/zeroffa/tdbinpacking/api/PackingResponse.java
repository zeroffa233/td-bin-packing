package com.zeroffa.tdbinpacking.api;

import java.util.List;

public class PackingResponse {
    private final int code;
    private final String msg;
    private final ResponseData data;
    private final int containerCount;
    private final List<Double> containerUtilizations;
    private final double averageUtilization;

    public PackingResponse(int code, String msg, ResponseData data) {
        this(code, msg, data, 0, java.util.Collections.<Double>emptyList(), 0.0D);
    }

    public PackingResponse(int code, String msg, ResponseData data, int containerCount,
                           List<Double> containerUtilizations, double averageUtilization) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.containerCount = containerCount;
        this.containerUtilizations = containerUtilizations;
        this.averageUtilization = averageUtilization;
    }

    public int code() { return code; }
    public String msg() { return msg; }
    public ResponseData data() { return data; }
    public int containerCount() { return containerCount; }
    public List<Double> containerUtilizations() { return containerUtilizations; }
    public double averageUtilization() { return averageUtilization; }
}
