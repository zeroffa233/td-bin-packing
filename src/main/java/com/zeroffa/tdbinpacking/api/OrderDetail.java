package com.zeroffa.tdbinpacking.api;

public class OrderDetail {
    private final long shipmentDetailId;
    private final String shipmentCode;
    private final Long waveId;
    private final String itemCode;
    private final String itemName;
    private final long quantity;
    private final String batch;
    private final String lot;
    private final String manufactureDate;
    private final String buildCode;
    private final String zoneCode;
    private final String stores;
    private final int carrierService;
    private final int isTaoX;
    private final int groupId;

    public OrderDetail(long shipmentDetailId, String shipmentCode, Long waveId, String itemCode, String itemName,
                       long quantity, String batch, String lot, String manufactureDate, String buildCode,
                       String zoneCode, String stores, int carrierService, int isTaoX, int groupId) {
        this.shipmentDetailId = shipmentDetailId;
        this.shipmentCode = shipmentCode;
        this.waveId = waveId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.quantity = quantity;
        this.batch = batch;
        this.lot = lot;
        this.manufactureDate = manufactureDate;
        this.buildCode = buildCode;
        this.zoneCode = zoneCode;
        this.stores = stores;
        this.carrierService = carrierService;
        this.isTaoX = isTaoX;
        this.groupId = groupId;
    }

    public long shipmentDetailId() { return shipmentDetailId; }
    public String shipmentCode() { return shipmentCode; }
    public Long waveId() { return waveId; }
    public String itemCode() { return itemCode; }
    public String itemName() { return itemName; }
    public long quantity() { return quantity; }
    public String batch() { return batch; }
    public String lot() { return lot; }
    public String manufactureDate() { return manufactureDate; }
    public String buildCode() { return buildCode; }
    public String zoneCode() { return zoneCode; }
    public String stores() { return stores; }
    public int carrierService() { return carrierService; }
    public int isTaoX() { return isTaoX; }
    public int groupId() { return groupId; }
}
