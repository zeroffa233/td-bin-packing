package com.zeroffa.tdbinpacking.api;

import java.util.List;

public class PackingRequest {
    private final List<RequestItem> items;
    private final List<RequestContainer> containers;
    private final List<OrderDetail> order;
    private final List<LclRule> lclRules;
    private final int unpackJudge;

    public PackingRequest(List<RequestItem> items,
                          List<RequestContainer> containers,
                          List<OrderDetail> order,
                          List<LclRule> lclRules,
                          int unpackJudge) {
        this.items = items;
        this.containers = containers;
        this.order = order;
        this.lclRules = lclRules;
        this.unpackJudge = unpackJudge;
    }

    public List<RequestItem> items() {
        return items;
    }

    public List<RequestContainer> containers() {
        return containers;
    }

    public List<OrderDetail> order() {
        return order;
    }

    public List<LclRule> lclRules() {
        return lclRules;
    }

    public int unpackJudge() {
        return unpackJudge;
    }
}
