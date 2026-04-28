package com.zeroffa.tdbinpacking.api;

import com.zeroffa.tdbinpacking.service.PackingService;

public class PackingApi {

    private final PackingService packingService;
    private final PackingJsonCodec jsonCodec;

    public PackingApi() {
        this(new PackingService(), new PackingJsonCodec());
    }

    public PackingApi(PackingService packingService, PackingJsonCodec jsonCodec) {
        this.packingService = packingService;
        this.jsonCodec = jsonCodec;
    }

    public String packJson(String inputJson) {
        PackingRequest request = jsonCodec.fromJson(inputJson);
        PackingResponse response = packingService.pack(request);
        return jsonCodec.toJson(response);
    }
}
