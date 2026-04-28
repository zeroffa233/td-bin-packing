package com.zeroffa.tdbinpacking.api;

public class LclRule {
    private final int sequence;
    private final String code;
    private final String name;

    public LclRule(int sequence, String code, String name) {
        this.sequence = sequence;
        this.code = code;
        this.name = name;
    }

    public int sequence() { return sequence; }
    public String code() { return code; }
    public String name() { return name; }
}
