public class ShippingService {
    private final DataPersistence dp;
    private final Log log;
    public ShippingService(DataPersistence dp, Log log) {
        this.dp = dp;
        this.log = log;
    }
    public Shipment createShipment(Order order) {
        String trackingId = generateTrackingId();
        Shipment sh = new Shipment();
        sh.trackingId = trackingId;
        sh.orderId = order.orderId;
        sh.status = "PACKED";
        dp.addShipment(sh);
        order.trackingId = trackingId;
        log.write(order.orderId, "SHIPMENT", "OK", "Created " + trackingId);
        return sh;
    }
    private String generateTrackingId() {
        String serial = Utils.padLeft(dp.trackingSerial++, 8);
        return "TRK-" + serial;
    }
}

