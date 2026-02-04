public class InventoryService {

    /** Validate inventory for the order and reserve stock if available. 
     *  Returns true if inventory check passes, false if order is cancelled due to invalid product or shortage. */
    public static boolean validateAndReserve(Order order, DataPersistence dp, Log log) {
        // Order validation: check each item exists and stock is sufficient
        for (int i = 0; i < order.itemCount; i++) {
            Item it = order.items[i];
            if (it == null) continue;
            Product prod = dp.findProductById(it.productId);
            if (prod == null) {
                // Product ID not found
                order.status = "CANCELLED";
                order.cancelReason = "Invalid product " + it.productId;
                log.write(order.orderId, "Order cancelled – " + order.cancelReason);
                return false;
            }
            if (prod.stock < it.quantity) {
                // Not enough stock
                order.status = "CANCELLED";
                order.cancelReason = "Inventory Shortage";
                log.write(order.orderId, "Order cancelled – " + order.cancelReason);
                return false;
            }
        }
        // If we reach here, all items are available. Deduct stock to reserve it.
        for (int i = 0; i < order.itemCount; i++) {
            Item it = order.items[i];
            Product prod = dp.findProductById(it.productId);
            if (prod != null) {
                prod.stock -= it.quantity;
            }
        }
        log.write(order.orderId, "Inventory OK – stock reserved");
        return true;
    }

    /** Add stock to a product (restock). Returns true if successful. */
    public static boolean restockProduct(String productId, int quantity, DataPersistence dp, Log log) {
        if (quantity <= 0) return false;
        Product prod = dp.findProductById(productId);
        if (prod == null) {
            return false;
        }
        prod.stock += quantity;
        // Log the restocking action as an admin event
        log.write("ADMIN", "Restocked product " + productId + " by " + quantity + " units");
        return true;
    }

    /** Check for low-stock items (stock < 5) and print alerts */
    public static void showLowStockAlerts(DataPersistence dp) {
        boolean anyLow = false;
        System.out.print("Low Stock Items (stock < 5):\n");
        for (int i = 0; i < dp.productCount; i++) {
            Product p = dp.products[i];
            if (p == null) continue;
            if (p.stock < 5) {
                anyLow = true;
                System.out.print(" - " + p.productId + " (" + p.name + ") stock = " + p.stock + "\n");
            }
        }
        if (!anyLow) {
            System.out.print(" (None)\n");
        }
    }
}


