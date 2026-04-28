package com.zeroffa.tdbinpacking.api;

import java.math.BigDecimal;

public class RequestItem {
    private final String code;
    private final String itemCategory1;
    private final String itemCategory2;
    private final BigDecimal weight;
    private final BigDecimal length;
    private final BigDecimal width;
    private final BigDecimal height;
    private final Long csQty;
    private final int allowDown;
    private final BigDecimal volume;
    private final String name;
    private final String brand;
    private final String itemSize;
    private final String itemColor;
    private final String itemStyle;

    public RequestItem(String code, String itemCategory1, String itemCategory2, BigDecimal weight,
                       BigDecimal length, BigDecimal width, BigDecimal height, Long csQty, int allowDown,
                       BigDecimal volume, String name, String brand, String itemSize, String itemColor,
                       String itemStyle) {
        this.code = code;
        this.itemCategory1 = itemCategory1;
        this.itemCategory2 = itemCategory2;
        this.weight = weight;
        this.length = length;
        this.width = width;
        this.height = height;
        this.csQty = csQty;
        this.allowDown = allowDown;
        this.volume = volume;
        this.name = name;
        this.brand = brand;
        this.itemSize = itemSize;
        this.itemColor = itemColor;
        this.itemStyle = itemStyle;
    }

    public String code() { return code; }
    public String itemCategory1() { return itemCategory1; }
    public String itemCategory2() { return itemCategory2; }
    public BigDecimal weight() { return weight; }
    public BigDecimal length() { return length; }
    public BigDecimal width() { return width; }
    public BigDecimal height() { return height; }
    public Long csQty() { return csQty; }
    public int allowDown() { return allowDown; }
    public BigDecimal volume() { return volume; }
    public String name() { return name; }
    public String brand() { return brand; }
    public String itemSize() { return itemSize; }
    public String itemColor() { return itemColor; }
    public String itemStyle() { return itemStyle; }
}
