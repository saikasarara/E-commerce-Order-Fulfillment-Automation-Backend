public class InventoryService {
    private final DataPersistence dp;
    private final Log log;
    public InventoryService(DataPersistence dp, Log log) {
        this.dp = dp;
        this.log = log;
    }
    public boolean verifyAndReserve(Order o) {
        // Check stock for all items
        int i = 0;
        while (i < o.itemCount) {
            Item it = o.items[i];
            Product p = dp.findProductById(it.productId);
            if (p == null) {
                log.write(o.orderId, "INVENTORY", "FAIL", "Unknown product " + it.productId);
                return false;
            }
            if (p.stock < it.qty) {
                log.write(o.orderId, "INVENTORY", "FAIL", "Insufficient stock " + it.productId);
                return false;
            }
            i++;
        }
        // Reserve stock
        i = 0;
        while (i < o.itemCount) {
            Item it = o.items[i];
            Product p = dp.findProductById(it.productId);
            p.stock -= it.qty;
            i++;
        }
        log.write(o.orderId, "INVENTORY", "OK", "Reserved stock");
        return true;
    }
    public void rollbackReservation(Order o) {
        int i = 0;
        while (i < o.itemCount) {
            Item it = o.items[i];
            Product p = dp.findProductById(it.productId);
            if (p != null) p.stock += it.qty;
            i++;
        }
        log.write(o.orderId, "INVENTORY", "OK", "Rollback stock");
    }
}

