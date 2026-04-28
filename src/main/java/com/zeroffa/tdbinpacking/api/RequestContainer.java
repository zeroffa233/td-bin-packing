package com.zeroffa.tdbinpacking.api;

import java.math.BigDecimal;

public class RequestContainer {
    private final String code;
    private final String name;
    private final BigDecimal length;
    private final BigDecimal width;
    private final BigDecimal height;
    private final BigDecimal maxWeight;
    private final BigDecimal capacityRate;
    private final int sequence;
    private final long quantity;
    private final String stores;

    public RequestContainer(String code, String name, BigDecimal length, BigDecimal width, BigDecimal height,
                            BigDecimal maxWeight, BigDecimal capacityRate, int sequence, long quantity,
                            String stores) {
        this.code = code;
        this.name = name;
        this.length = length;
        this.width = width;
        this.height = height;
        this.maxWeight = maxWeight;
        this.capacityRate = capacityRate;
        this.sequence = sequence;
        this.quantity = quantity;
        this.stores = stores;
    }

    public String code() { return code; }
    public String name() { return name; }
    public BigDecimal length() { return length; }
    public BigDecimal width() { return width; }
    public BigDecimal height() { return height; }
    public BigDecimal maxWeight() { return maxWeight; }
    public BigDecimal capacityRate() { return capacityRate; }
    public int sequence() { return sequence; }
    public long quantity() { return quantity; }
    public String stores() { return stores; }
}
