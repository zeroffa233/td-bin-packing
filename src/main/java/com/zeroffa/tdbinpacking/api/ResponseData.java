package com.zeroffa.tdbinpacking.api;

import java.util.List;

public class ResponseData {
    private final List<ResponseInfo> infos;

    public ResponseData(List<ResponseInfo> infos) {
        this.infos = infos;
    }

    public List<ResponseInfo> infos() {
        return infos;
    }
}
